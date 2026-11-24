import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'offline.dart';

typedef Json = Map<String, dynamic>;

/// Devise d'affichage des montants (réglage de la ferme, mis à jour par l'écran principal).
String deviseActuelle = 'FCFA';

/// URL du backend. Émulateur Android : http://10.0.2.2:8080
/// Autre : flutter run --dart-define=API_URL=https://ton-backend.onrender.com
const apiUrl = String.fromEnvironment('API_URL', defaultValue: 'http://10.0.2.2:8080');

final prefsProvider = Provider<SharedPreferences>((_) => throw UnimplementedError());

/// Thème : clair, sombre ou automatique (celui du téléphone). Mémorisé sur l'appareil.
final themeModeProvider = StateProvider<ThemeMode>((ref) {
  switch (ref.read(prefsProvider).getString('theme')) {
    case 'dark':
      return ThemeMode.dark;
    case 'light':
      return ThemeMode.light;
    default:
      return ThemeMode.system;
  }
});

// ---------------------------------------------------------------- Auth
class AuthState {
  final String? token, nom, role;
  const AuthState(this.token, this.nom, [this.role]);
}

class AuthNotifier extends StateNotifier<AuthState> {
  AuthNotifier(this.prefs) : super(AuthState(prefs.getString('token'), prefs.getString('nom'), prefs.getString('role')));
  final SharedPreferences prefs;

  Future<void> _auth(String path, Json body) async {
    final r = await Dio(BaseOptions(baseUrl: apiUrl)).post(path, data: body);
    final d = r.data as Map;
    await prefs.setString('token', d['token'] as String);
    await prefs.setString('nom', (d['nom'] ?? '') as String);
    await prefs.setString('role', (d['role'] ?? 'PROPRIETAIRE') as String);
    state = AuthState(d['token'] as String, (d['nom'] ?? '') as String, (d['role'] ?? 'PROPRIETAIRE') as String);
  }

  Future<void> login(String email, String pwd) => _auth('/api/auth/login', {'email': email, 'motDePasse': pwd});
  Future<void> register(String nom, String email, String pwd) =>
      _auth('/api/auth/register', {'nom': nom, 'email': email, 'motDePasse': pwd});

  Future<void> demanderCode(String email) async {
    await Dio(BaseOptions(baseUrl: apiUrl)).post('/api/auth/mot-de-passe-oublie', data: {'email': email});
  }

  Future<void> reinitialiser(String email, String code, String nouveau) async {
    await Dio(BaseOptions(baseUrl: apiUrl)).post('/api/auth/reinitialiser', data: {'email': email, 'code': code, 'nouveau': nouveau});
  }

  Future<void> logout() async {
    await prefs.remove('token');
    await prefs.remove('nom');
    await prefs.remove('role');
    for (final k in prefs.getKeys().where((k) => k.startsWith('cache:') || k == 'queue').toList()) {
      await prefs.remove(k);
    }
    state = const AuthState(null, null);
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) => AuthNotifier(ref.read(prefsProvider)));

// ---------------------------------------------------------------- HTTP
final dioProvider = Provider<Dio>((ref) {
  final token = ref.watch(authProvider.select((a) => a.token));
  final d = Dio(BaseOptions(
    baseUrl: apiUrl,
    connectTimeout: const Duration(seconds: 20),
    receiveTimeout: const Duration(seconds: 30),
  ));
  d.interceptors.add(InterceptorsWrapper(
    onRequest: (o, h) {
      if (token != null) o.headers['Authorization'] = 'Bearer $token';
      h.next(o);
    },
    onError: (e, h) {
      if (e.response?.statusCode == 401) ref.read(authProvider.notifier).logout();
      h.next(e);
    },
  ));
  d.interceptors.add(ref.watch(offlineProvider));
  return d;
});

extension DioJson on Dio {
  Future<List<Json>> list(String p) async => ((await get(p)).data as List).cast<Json>();
  Future<Json> obj(String p) async => Json.from((await get(p)).data as Map);
}

String errMsg(Object e) {
  if (e is DioException) {
    final d = e.response?.data;
    if (e.response?.statusCode == 403) return 'Action non autorisée pour votre rôle';
    if (d is Map && d['message'] != null) return d['message'].toString();
    return e.message ?? 'Erreur réseau';
  }
  return e.toString();
}

// ---------------------------------------------------------------- Données (se rechargent à chaque "tick")
final tickProvider = StateProvider<int>((_) => 0);

FutureProvider<List<Json>> _lp(String p) => FutureProvider<List<Json>>((ref) {
      ref.watch(tickProvider);
      return ref.watch(dioProvider).list(p);
    });

final lapinsP = _lp('/api/lapins');
final cagesP = _lp('/api/cages/plan');
final sailliesP = _lp('/api/saillies');
final porteesP = _lp('/api/portees');
final lotsP = _lp('/api/lots');
final casP = _lp('/api/maladies');
final stockP = _lp('/api/stock');
final mouvementsP = _lp('/api/mouvements');
final guideP = _lp('/api/maladies/guide');
final soinsP = _lp('/api/soins');
final clientsP = _lp('/api/clients');
final commandesP = _lp('/api/commandes');
final equipeP = _lp('/api/equipe');
final configP = FutureProvider<Json>((ref) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).obj('/api/config');
});
final tachesP = _lp('/api/taches/aujourdhui');
final toutesTachesP = _lp('/api/taches');
final photoP = FutureProvider.family<Uint8List?, int>((ref, id) async {
  final r = await ref.watch(dioProvider).get('/api/lapins/$id/photo');
  final p = (r.data as Map)['photo'];
  return p == null ? null : base64Decode(p as String);
});
final statsP = FutureProvider<Json>((ref) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).obj('/api/statistiques');
});

final dashP = FutureProvider<Json>((ref) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).obj('/api/dashboard');
});
final bilanP = FutureProvider<Json>((ref) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).obj('/api/bilan?jours=30');
});
final peseesP = FutureProvider.family<List<Json>, int>((ref, id) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).list('/api/lapins/$id/pesees');
});

// ---------------------------------------------------------------- Temps réel (SSE + secours par rafraîchissement)
final realtimeProvider = Provider<void>((ref) {
  if (ref.watch(authProvider.select((a) => a.token)) == null) return;
  final dio = ref.watch(dioProvider);
  var active = true;

  void bump() {
    if (active) ref.read(tickProvider.notifier).state++;
  }

  final timer = Timer.periodic(const Duration(seconds: 45), (_) {
    ref.read(offlineProvider).flush();
    bump();
  });

  Future<void> loop() async {
    while (active) {
      try {
        final r = await dio.get<ResponseBody>('/api/alertes/stream',
            options: Options(
              responseType: ResponseType.stream,
              receiveTimeout: Duration.zero,
              headers: {'Accept': 'text/event-stream'},
            ));
        await for (final line in r.data!.stream.cast<List<int>>().transform(utf8.decoder).transform(const LineSplitter())) {
          if (!active) break;
          if (line.startsWith('data:')) bump();
        }
      } catch (_) {}
      if (active) await Future.delayed(const Duration(seconds: 5));
    }
  }

  loop();
  ref.onDispose(() {
    active = false;
    timer.cancel();
  });
});

// ---------------------------------------------------------------- Utilitaires
String fcfa(num x) => '${x.round().toString().replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => ' ')} $deviseActuelle';

String dm(dynamic iso) {
  if (iso == null) return '-';
  final d = DateTime.tryParse(iso.toString());
  return d == null ? '-' : '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}';
}

String today() => DateTime.now().toIso8601String().substring(0, 10);
int daysSince(dynamic iso) => DateTime.now().difference(DateTime.tryParse(iso?.toString() ?? '') ?? DateTime.now()).inDays;
String nomDe(List<Json> ls, dynamic id) => (ls.firstWhere((l) => l['id'] == id, orElse: () => <String, dynamic>{'nom': '?'})['nom'] ?? '?').toString();

Color sevColor(String s) => s == 'red' ? const Color(0xFFC0402B) : s == 'amb' ? const Color(0xFFB97B0B) : const Color(0xFF2F6A9E);

/// Exécute une action API puis rafraîchit toutes les données.
Future<void> run(BuildContext c, WidgetRef ref, Future<dynamic> Function() f) async {
  final tick = ref.read(tickProvider.notifier);
  final messenger = ScaffoldMessenger.of(c);
  try {
    await f();
    tick.state++;
  } catch (e) {
    messenger.showSnackBar(SnackBar(content: Text(errMsg(e))));
  }
}
