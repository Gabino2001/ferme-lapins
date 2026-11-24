import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../qr.dart';
import '../ui.dart';

class CagesScreen extends ConsumerWidget {
  const CagesScreen({super.key});

  Color _bg(String e) => switch (e) {
        'red' => const Color(0x40C0402B),
        'amb' => const Color(0x40B97B0B),
        'ok' => const Color(0x402F6B4F),
        _ => Colors.transparent,
      };

  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
        backgroundColor: Colors.transparent,
        floatingActionButton: FloatingActionButton.extended(
          icon: const Icon(Icons.add),
          label: const Text('Cage'),
          onPressed: () async {
            final r = await showForm(context, 'Nouvelle cage', [
              const F('numero', 'Numéro (ex. B4)'),
              const F('type', 'Type', type: 'select', opts: [['Reproduction', 'Reproduction'], ['Maternité', 'Maternité'], ['Engraissement', 'Engraissement'], ['Quarantaine', 'Quarantaine']]),
              const F('capacite', 'Capacité', type: 'number', init: 1),
            ]);
            if (r != null && r['numero'] != null && context.mounted) await run(context, ref, () => ref.read(dioProvider).post('/api/cages', data: r));
          },
        ),
        body: view(ref.watch(cagesP), (cages) {
          if (cages.isEmpty) return const Padding(padding: EdgeInsets.all(16), child: Empty('Aucune cage. Créez vos cages pour voir le plan de la ferme.'));
          final libres = cages.where((c) => c['occupation'] == 0).length;
          return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 90), children: [
            Section('Plan de la ferme', hint: '${cages.length} cages · $libres libres'),
            GridView.count(
              crossAxisCount: 3,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              mainAxisSpacing: 8,
              crossAxisSpacing: 8,
              childAspectRatio: .82,
              children: [
                for (final c in cages)
                  InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => showQr(context, 'Cage ${c['numero']}', 'ferme:cage:${c['numero']}'),
                    onLongPress: () async {
                      final ok = await showDialog<bool>(
                        context: context,
                        builder: (_) => AlertDialog(
                          title: Text('Supprimer la cage ${c['numero']} ?'),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
                            FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Supprimer')),
                          ],
                        ),
                      );
                      if (ok == true && context.mounted) await run(context, ref, () => ref.read(dioProvider).delete('/api/cages/${c['id']}'));
                    },
                    child: Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: _bg(c['etat']),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: kLine),
                      ),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text('${c['numero']}  ${c['occupation']}/${c['capacite']}', style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                        Text('${c['type'] ?? ''}', style: const TextStyle(fontSize: 12, color: kMute)),
                        Text('${c['libelle']}', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                        Text((c['occupants'] as List).join(', '), maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: kMute)),
                      ]),
                    ),
                  ),
              ],
            ),
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Text('Vert : occupée · orange : à surveiller · rouge : problème · transparent : libre.\nTouchez une cage pour afficher son code QR. Appui long pour la supprimer. Pour déplacer un lapin, ouvrez sa fiche puis « Modifier la fiche ».',
                  style: TextStyle(fontSize: 13, color: kMute)),
            ),
          ]);
        }),
      );
}
