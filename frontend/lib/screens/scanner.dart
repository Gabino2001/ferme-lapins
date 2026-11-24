import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import '../core.dart';
import 'lapins.dart';

/// Scanne un code QR de cage ou de lapin et ouvre la fiche correspondante.
class ScannerScreen extends ConsumerStatefulWidget {
  const ScannerScreen({super.key});
  @override
  ConsumerState<ScannerScreen> createState() => _ScannerState();
}

class _ScannerState extends ConsumerState<ScannerScreen> {
  bool traite = false;
  final saisie = TextEditingController();

  void msg(String t) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(t)));

  Future<void> ouvrir(String code) async {
    final p = code.trim().split(':');
    if (p.length != 3 || p[0] != 'ferme') {
      msg('Code non reconnu');
      traite = false;
      return;
    }
    if (p[1] == 'lapin') {
      final id = int.tryParse(p[2]);
      if (id == null) {
        msg('Code non reconnu');
        traite = false;
        return;
      }
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => LapinDetail(id)));
      return;
    }
    if (p[1] == 'cage') {
      final cages = await ref.read(cagesP.future);
      final c = cages.where((x) => '${x['numero']}' == p[2]).toList();
      if (!mounted) return;
      if (c.isEmpty) {
        msg('Cage introuvable');
        traite = false;
        return;
      }
      final g = c.first;
      await showDialog(
        context: context,
        builder: (_) => AlertDialog(
          title: Text('Cage ${g['numero']}'),
          content: Text('${g['type'] ?? ''} · ${g['occupation']}/${g['capacite']}\n${g['libelle']}\n${(g['occupants'] as List).join(', ')}'),
          actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('OK'))],
        ),
      );
      if (mounted) Navigator.pop(context);
      return;
    }
    msg('Code non reconnu');
    traite = false;
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Scanner un code QR')),
        body: Column(children: [
          Expanded(
            child: kIsWeb
                ? const Center(child: Padding(padding: EdgeInsets.all(24), child: Text('Le scan par caméra est disponible sur Android et iOS. Saisissez le code ci-dessous.', textAlign: TextAlign.center)))
                : MobileScanner(onDetect: (capture) {
                    if (traite) return;
                    final v = capture.barcodes.isEmpty ? null : capture.barcodes.first.rawValue;
                    if (v == null) return;
                    traite = true;
                    ouvrir(v);
                  }),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              controller: saisie,
              decoration: InputDecoration(
                labelText: 'Ou saisissez le code (ex. ferme:cage:A1)',
                suffixIcon: IconButton(icon: const Icon(Icons.arrow_forward), onPressed: () => ouvrir(saisie.text)),
              ),
              onSubmitted: ouvrir,
            ),
          ),
        ]),
      );
}
