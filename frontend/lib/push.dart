import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core.dart';

/// Notifications push (Android / iOS). Sans configuration Firebase, la fonction échoue en silence et l'application continue normalement.
Future<void> initPush(WidgetRef ref) async {
  if (kIsWeb) return;
  if (defaultTargetPlatform != TargetPlatform.android && defaultTargetPlatform != TargetPlatform.iOS) return;
  try {
    await Firebase.initializeApp();
    final messaging = FirebaseMessaging.instance;
    await messaging.requestPermission();
    final dio = ref.read(dioProvider);
    Future<void> envoyer(String token) =>
        dio.post('/api/appareils', data: {'token': token, 'plateforme': defaultTargetPlatform.name});
    final token = await messaging.getToken();
    if (token != null) await envoyer(token);
    messaging.onTokenRefresh.listen((t) => envoyer(t).catchError((_) {}));
  } catch (_) {
    // Firebase non configuré : pas de notifications, aucune incidence sur le reste
  }
}
