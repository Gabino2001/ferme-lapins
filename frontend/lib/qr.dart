import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'ui.dart';

/// Affiche le code QR d'une cage ou d'un lapin (format ferme:cage:A1 ou ferme:lapin:12).
Future<void> showQr(BuildContext context, String titre, String data) => showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(titre),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          SizedBox(width: 220, height: 220, child: QrImageView(data: data, backgroundColor: Colors.white)),
          const SizedBox(height: 8),
          Text(data, style: const TextStyle(fontSize: 12, color: kMute)),
          const SizedBox(height: 8),
          const Text('Imprimez ce code et collez-le : en le scannant depuis l’application, la fiche s’ouvre.',
              textAlign: TextAlign.center, style: TextStyle(fontSize: 12)),
        ]),
        actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Fermer'))],
      ),
    );
