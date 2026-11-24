import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../genealogie.dart';
import '../ui.dart';

final delaiP = StateProvider<int>((_) => 21);
final planningP = FutureProvider<List<Json>>((ref) {
  ref.watch(tickProvider);
  final d = ref.watch(delaiP);
  return ref.watch(dioProvider).list('/api/planning?delai=$d');
});

class PlanningScreen extends ConsumerWidget {
  const PlanningScreen({super.key});

  static const _etats = {
    'PRETE': 'Prête',
    'GESTANTE': 'Gestante',
    'ALLAITANTE': 'Allaitante',
    'REPOS': 'Au repos',
    'TROP_JEUNE': 'Trop jeune',
  };
  static const _couleurs = {
    'PRETE': Color(0xFF2F6B4F),
    'GESTANTE': Color(0xFF2F6A9E),
    'ALLAITANTE': Color(0xFFB97B0B),
    'REPOS': Colors.grey,
    'TROP_JEUNE': Colors.grey,
  };
  static const _parente = {'AUCUN': 'aucun lien connu', 'FAIBLE': 'éloignée', 'MOYEN': 'moyenne', 'ELEVE': 'élevée'};

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final delai = ref.watch(delaiP);
    final dio = ref.read(dioProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Planning de reproduction')),
      body: view(ref.watch(planningP), (lignes) {
        int nb(String e) => lignes.where((l) => l['etat'] == e).length;
        return RefreshIndicator(
          onRefresh: () async => ref.read(tickProvider.notifier).state++,
          child: ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 24), children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: DropdownButtonFormField<int>(
                  initialValue: delai,
                  decoration: const InputDecoration(labelText: 'Rythme : ressaillir la lapine après la mise bas'),
                  items: const [
                    DropdownMenuItem(value: 11, child: Text('11 jours (intensif)')),
                    DropdownMenuItem(value: 21, child: Text('21 jours (semi-intensif)')),
                    DropdownMenuItem(value: 42, child: Text('42 jours (extensif)')),
                  ],
                  onChanged: (v) {
                    if (v != null) ref.read(delaiP.notifier).state = v;
                  },
                ),
              ),
            ),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 4),
              child: Text('Repères généraux, à adapter à votre race et à votre bâtiment.', style: TextStyle(fontSize: 12, color: kMute)),
            ),
            Section('Lapines', hint: '${nb('PRETE')} prêtes · ${nb('GESTANTE')} gestantes · ${nb('ALLAITANTE')} allaitantes'),
            if (lignes.isEmpty) const Empty('Aucune femelle enregistrée.'),
            for (final l in lignes)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Row(children: [
                      Expanded(child: Text('${l['nom']} · cage ${l['cage'] ?? '-'}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
                      Pill(_etats['${l['etat']}'] ?? '${l['etat']}', _couleurs['${l['etat']}'] ?? Colors.grey),
                    ]),
                    Text('${l['detail']}', style: const TextStyle(color: kMute)),
                    if (l['conseil'] != null) Padding(padding: const EdgeInsets.only(top: 4), child: Text('${l['conseil']}', style: const TextStyle(color: Color(0xFFB97B0B)))),
                    if (l['etat'] == 'PRETE') ...[
                      if (l['maleNom'] != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text('Mâle conseillé : ${l['maleNom']} (parenté : ${_parente['${l['niveauParente']}'] ?? ''})'),
                        ),
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: FilledButton(
                          onPressed: () async {
                            final ls = ref.read(lapinsP).valueOrNull ?? <Json>[];
                            final r = await showForm(context, 'Saillie de ${l['nom']}', [
                              F('maleId', 'Mâle', type: 'select', opts: lapinOptions(ls, sexe: 'M'), init: l['maleId']?.toString()),
                              const F('date', 'Date', type: 'date'),
                            ]);
                            if (r == null || r['maleId'] == null || !context.mounted) return;
                            if (!await confirmerParente(context, dio, l['femelleId'], r['maleId'])) return;
                            if (context.mounted) {
                              await run(context, ref, () => dio.post('/api/saillies', data: {'femelleId': l['femelleId'], 'maleId': r['maleId'], 'date': r['date']}));
                            }
                          },
                          child: const Text('Enregistrer une saillie'),
                        ),
                      ),
                    ],
                  ]),
                ),
              ),
          ]),
        );
      }),
    );
  }
}
