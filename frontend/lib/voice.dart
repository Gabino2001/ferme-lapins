import 'package:flutter/foundation.dart';
import 'package:speech_to_text/speech_to_text.dart';

/// Saisie vocale en français (si le téléphone propose la langue, sinon la langue du système).
class Voice {
  final SpeechToText _s = SpeechToText();
  bool _pret = false;
  String? _locale;
  VoidCallback? _fin;

  Future<bool> demarrer(void Function(String) onTexte, {VoidCallback? onFin}) async {
    _fin = onFin;
    try {
      if (!_pret) {
        _pret = await _s.initialize(
          onStatus: (s) {
            if (s == 'done' || s == 'notListening') _fin?.call();
          },
          onError: (_) => _fin?.call(),
        );
        if (_pret) {
          for (final l in await _s.locales()) {
            if (l.localeId.toLowerCase().startsWith('fr')) {
              _locale = l.localeId;
              break;
            }
          }
        }
      }
      if (!_pret) return false;
      await _s.listen(onResult: (r) => onTexte(r.recognizedWords), localeId: _locale);
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<void> arreter() async {
    try {
      await _s.stop();
    } catch (_) {}
  }
}
