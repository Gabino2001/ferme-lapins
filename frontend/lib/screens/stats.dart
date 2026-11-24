import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';

class StatsScreen extends ConsumerWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
        appBar: AppBar(title: const Text('Indicateurs de performance')),
        body: view(ref.watch(statsP), (s) {
          final mensuel = (s['mensuel'] as List).cast<Json>();
          final classement = (s['classement'] as List).cast<Json>();
          final mx = mensuel.fold<double>(1, (m, x) => ((x['resultat'] as num).abs() > m) ? (x['resultat'] as num).abs().toDouble() : m);
          final mortalite = (s['mortaliteSevrage'] as num).toDouble();
          Widget tuile(String valeur, String label) => Expanded(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(children: [
                      Text(valeur, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                      Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, color: kMute)),
                    ]),
                  ),
                ),
              );
          return RefreshIndicator(
            onRefresh: () async => ref.read(tickProvider.notifier).state++,
            child: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
              const Section('Reproduction'),
              Row(children: [
                tuile('${s['tailleMoyennePortee']}', 'Nés par portée (moyenne)'),
                tuile('${s['vivantsMoyens']}', 'Vivants par portée'),
              ]),
              Row(children: [
                tuile('$mortalite %', 'Mortalité naissance → sevrage'),
                tuile('${s['nbPortees']}', 'Portées enregistrées'),
              ]),
              if (mortalite > 15)
                const Padding(
                  padding: EdgeInsets.only(top: 4),
                  child: Text('Mortalité élevée avant sevrage : vérifiez la température, l’hygiène des nids et l’alimentation des lapines allaitantes.',
                      style: TextStyle(color: Color(0xFFB97B0B))),
                ),
              const Section('Coûts', hint: '90 derniers jours'),
              Row(children: [
                tuile(fcfa(s['coutAlimentParLapin'] as num), 'Aliment par lapin vendu'),
                tuile('${s['lapinsVendus90j']}', 'Lapins vendus'),
              ]),
              const Section('Gain ou perte par mois'),
              for (final m in mensuel)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(children: [
                    SizedBox(width: 64, child: Text('${m['mois']}', style: const TextStyle(fontSize: 12))),
                    Expanded(
                      child: LinearProgressIndicator(
                        value: ((m['resultat'] as num).abs() / mx).clamp(0, 1).toDouble(),
                        minHeight: 10,
                        borderRadius: BorderRadius.circular(8),
                        backgroundColor: Colors.black12,
                        color: (m['resultat'] as num) >= 0 ? const Color(0xFF2F6B4F) : const Color(0xFFC0402B),
                      ),
                    ),
                    SizedBox(width: 104, child: Text(fcfa(m['resultat'] as num), textAlign: TextAlign.right, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600))),
                  ]),
                ),
              const Section('Meilleures lapines', hint: 'par lapereaux sevrés'),
              if (classement.isEmpty) const Empty('Enregistrez des portées pour voir le classement.'),
              for (var i = 0; i < classement.length; i++)
                Card(
                  child: ListTile(
                    leading: CircleAvatar(child: Text('${i + 1}')),
                    title: Text('${classement[i]['nom']}', style: const TextStyle(fontWeight: FontWeight.w600)),
                    subtitle: Text('${classement[i]['portees']} portée(s) · ${classement[i]['moyenneNes']} nés en moyenne'),
                    trailing: Text('${classement[i]['sevres']} sevrés', style: const TextStyle(fontWeight: FontWeight.w700)),
                  ),
                ),
            ]),
          );
        }),
      );
}
