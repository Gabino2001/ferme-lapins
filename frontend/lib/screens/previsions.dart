import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';

final previsionsP = FutureProvider<Json>((ref) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).obj('/api/previsions');
});

class PrevisionsScreen extends ConsumerWidget {
  const PrevisionsScreen({super.key});

  static const _icones = {
    'MISE_BAS': Icons.child_care,
    'SEVRAGE': Icons.call_split,
    'VENTE': Icons.sell_outlined,
    'SOIN': Icons.vaccines_outlined,
    'LIVRAISON': Icons.local_shipping_outlined,
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
        appBar: AppBar(title: const Text('Prévisions à 30 jours')),
        body: view(ref.watch(previsionsP), (p) {
          final evenements = (p['evenements'] as List).cast<Json>();
          final ruptures = (p['ruptures'] as List).cast<Json>();
          final revenu = p['revenuEstime'] as num?;
          final attente = p['commandesEnAttente'] as num?;
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
              const Section('En bref'),
              Row(children: [
                tuile('${p['lapereauxAttendus']}', 'Lapereaux attendus'),
                tuile('${p['lapinsPretsAVendre']}', 'Lapins prêts à vendre'),
              ]),
              if (revenu != null || attente != null)
                Row(children: [
                  if (revenu != null) tuile(fcfa(revenu), 'Ventes estimées (prix moyen des 90 derniers jours)'),
                  if (attente != null) tuile(fcfa(attente), 'Commandes en attente'),
                ]),
              const Section('Agenda', hint: '30 prochains jours'),
              if (evenements.isEmpty) const Empty('Rien de prévu dans les 30 prochains jours.'),
              for (final e in evenements)
                Card(
                  child: ListTile(
                    leading: Icon(_icones['${e['type']}'] ?? Icons.event),
                    title: Text('${e['titre']}', style: const TextStyle(fontWeight: FontWeight.w600)),
                    subtitle: Text('${e['detail']}'),
                    trailing: Text(dm(e['date']), style: const TextStyle(fontWeight: FontWeight.w700)),
                  ),
                ),
              const Section('Risques de rupture de stock'),
              if (ruptures.isEmpty) const Empty('Aucune rupture prévue dans les 60 jours.'),
              for (final r in ruptures)
                Card(
                  child: ListTile(
                    leading: Icon(Icons.inventory_2_outlined, color: (r['jours'] as num) < 5 ? const Color(0xFFC0402B) : null),
                    title: Text('${r['nom']}'),
                    subtitle: Text('Fin de stock vers le ${dm(r['date'])}'),
                    trailing: Text('${r['jours']} j', style: const TextStyle(fontWeight: FontWeight.w700)),
                  ),
                ),
              const Padding(
                padding: EdgeInsets.only(top: 8),
                child: Text('Prévisions calculées à partir de vos saillies, portées, lots et stocks : elles s’affinent à mesure que vous saisissez vos données.', style: TextStyle(fontSize: 12, color: kMute)),
              ),
            ]),
          );
        }),
      );
}
