import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'core.dart';

/// true quand la dernière requête a échoué faute de réseau.
final offlineStateProvider = StateProvider<bool>((_) => false);

/// Nombre de modifications enregistrées sur le téléphone et pas encore envoyées.
final pendingProvider = StateProvider<int>((_) => 0);

final offlineProvider = Provider<OfflineSync>((ref) {
  final token = ref.watch(authProvider.select((a) => a.token));
  return OfflineSync(ref, ref.read(prefsProvider), token);
});

/// Mode hors-ligne :
///  - les lectures (GET) réussies sont mises en cache et servies telles quelles quand le réseau tombe ;
///  - les écritures (POST/PUT/DELETE) faites sans réseau sont mises en file d'attente puis envoyées au retour du réseau.
class OfflineSync extends Interceptor {
  OfflineSync(this.ref, this.prefs, this.token) {
    raw = Dio(BaseOptions(
      baseUrl: apiUrl,
      headers: {if (token != null) 'Authorization': 'Bearer $token'},
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 30),
    ));
    Future.microtask(_syncCount);
  }

  final Ref ref;
  final SharedPreferences prefs;
  final String? token;
  late final Dio raw;
  bool _flushing = false;

  static bool isNet(DioException e) =>
      e.response == null &&
      (e.type == DioExceptionType.connectionError ||
          e.type == DioExceptionType.connectionTimeout ||
          e.type == DioExceptionType.sendTimeout ||
          e.type == DioExceptionType.receiveTimeout ||
          e.type == DioExceptionType.unknown);

  bool _skip(RequestOptions o) =>
      o.responseType == ResponseType.stream || o.path.contains('/stream') || o.path.contains('/photo') || o.path.contains('/api/auth') || o.path.contains('/api/assistant') || o.path.contains('/api/export');

  String _ck(RequestOptions o) => 'cache:${o.path}?${o.queryParameters}';

  List<Map<String, dynamic>> _queue() {
    final s = prefs.getString('queue');
    if (s == null) return [];
    return (jsonDecode(s) as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  Future<void> _save(List<Map<String, dynamic>> q) async {
    await prefs.setString('queue', jsonEncode(q));
    _syncCount();
  }

  void _syncCount() => Future.microtask(() {
        try {
          ref.read(pendingProvider.notifier).state = _queue().length;
        } catch (_) {}
      });

  void _setOffline(bool v) => Future.microtask(() {
        try {
          final n = ref.read(offlineStateProvider.notifier);
          if (n.state != v) n.state = v;
        } catch (_) {}
      });

  @override
  void onResponse(Response r, ResponseInterceptorHandler h) {
    final o = r.requestOptions;
    if (!_skip(o)) {
      _setOffline(false);
      if (o.method == 'GET' && (r.data is List || r.data is Map)) {
        try {
          prefs.setString(_ck(o), jsonEncode(r.data));
        } catch (_) {}
      }
      if (_queue().isNotEmpty) flush();
    }
    h.next(r);
  }

  @override
  void onError(DioException e, ErrorInterceptorHandler h) async {
    final o = e.requestOptions;
    if (isNet(e) && !_skip(o)) {
      _setOffline(true);
      if (o.method == 'GET') {
        final c = prefs.getString(_ck(o));
        if (c != null) {
          h.resolve(Response(requestOptions: o, data: jsonDecode(c), statusCode: 200));
          return;
        }
      } else {
        try {
          final q = _queue()..add({'m': o.method, 'p': o.path, 'd': o.data, 'q': o.queryParameters});
          await _save(q);
          h.resolve(Response(requestOptions: o, data: {'queued': true}, statusCode: 202));
          return;
        } catch (_) {}
      }
    }
    h.next(e);
  }

  /// Envoie la file d'attente dans l'ordre. S'arrête si le réseau est toujours absent.
  Future<void> flush() async {
    if (_flushing || token == null) return;
    _flushing = true;
    var envoye = false;
    try {
      var q = _queue();
      while (q.isNotEmpty) {
        final it = q.first;
        try {
          await raw.request(
            it['p'] as String,
            data: it['d'],
            queryParameters: Map<String, dynamic>.from((it['q'] ?? {}) as Map),
            options: Options(method: it['m'] as String),
          );
          envoye = true;
        } on DioException catch (e) {
          if (isNet(e)) {
            _setOffline(true);
            break;
          }
          // erreur du serveur (donnée refusée) : on abandonne cette modification
        }
        q = q.sublist(1);
        await _save(q);
      }
    } finally {
      _flushing = false;
    }
    if (envoye) {
      _setOffline(false);
      Future.microtask(() {
        try {
          ref.read(tickProvider.notifier).state++;
        } catch (_) {}
      });
    }
  }
}
