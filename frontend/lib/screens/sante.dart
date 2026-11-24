import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';

class SanteScreen extends ConsumerWidget {
  const SanteScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ls = ref.watch(lapinsP).valueOrNull ?? <Json>[];
    final cas = ref.watch(casP).valueOrNull ?? <Json>[];
    final guide = ref.watch(guideP).valueOrNull ?? <Json>[];
    final soins = ref.watch(soinsP).valueOrNull ?? <Json>[];
    final dio = ref.read(dioProvider);
    final encours = cas.where((c) => c['statut'] == 'EN_TRAITEMENT').length;

    return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
      Section('Cas de maladie', hint: '$encours en cours'),
      if (cas.isEmpty) const Empty('Aucun cas enregistré.'),
      for (final c in cas)
        Builder(builder: (_) {
          final on = c['statut'] == 'EN_TRAITEMENT';
          final g = guide.where((x) => x['nom'] == c['maladie']);
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Expanded(child: Text('${c['maladie']} · ${nomDe(ls, c['lapinId'])}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
                  Pill(on ? 'En traitement' : c['statut'] == 'GUERI' ? 'Guéri' : 'Décédé', on ? const Color(0xFFC0402B) : Colors.grey),
                ]),
                Text('Depuis le ${dm(c['date'])}', style: const TextStyle(color: kMute)),
                if (on && g.isNotEmpty) Bullets('Traitement conseillé', g.first['solutions'] as List),
                if (on)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Wrap(spacing: 8, children: [
                      FilledButton(onPressed: () => run(context, ref, () => dio.post('/api/maladies/${c['id']}/statut', queryParameters: {'statut': 'GUERI'})), child: const Text('Guéri')),
                      OutlinedButton(onPressed: () => run(context, ref, () => dio.post('/api/maladies/${c['id']}/statut', queryParameters: {'statut': 'DECEDE'})), child: const Text('Décédé')),
                    ]),
                  ),
              ]),
            ),
          );
        }),
      OutlinedButton.icon(
        icon: const Icon(Icons.add),
        label: const Text('Déclarer un cas'),
        onPressed: () async {
          final r = await showForm(context, 'Déclarer un cas', [
            F('lapinId', 'Lapin', type: 'select', opts: lapinOptions(ls)),
            F('maladie', 'Maladie', type: 'select', opts: [for (final g in guide) [g['nom'].toString(), g['nom'].toString()]]),
            const F('date', 'Date', type: 'date'),
          ]);
          if (r != null && r['lapinId'] != null && r['maladie'] != null && context.mounted) {
            await run(context, ref, () => dio.post('/api/maladies', data: {...r, 'statut': 'EN_TRAITEMENT'}));
          }
        },
      ),
      Section('Vaccins et traitements', hint: '${soins.where((s) => s['dateRealisee'] == null).length} à faire'),
      if (soins.isEmpty) const Empty('Aucun soin planifié.'),
      for (final s in (soins.toList()..sort((a, b) => '${a['dateRealisee'] != null}${a['datePrevue']}'.compareTo('${b['dateRealisee'] != null}${b['datePrevue']}'))))
        Builder(builder: (_) {
          final fait = s['dateRealisee'] != null;
          final retard = !fait && DateTime.parse(s['datePrevue']).isBefore(DateTime.now().subtract(const Duration(days: 1)));
          final cible = s['lapinId'] == null ? 'Tout le cheptel' : nomDe(ls, s['lapinId']);
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Expanded(child: Text('${s['type'] == 'VACCIN' ? 'Vaccin' : 'Traitement'} · ${s['nom']}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
                  Pill(fait ? 'Fait le ${dm(s['dateRealisee'])}' : retard ? 'En retard' : 'Prévu le ${dm(s['datePrevue'])}',
                      fait ? Colors.grey : retard ? const Color(0xFFC0402B) : const Color(0xFF2F6A9E)),
                ]),
                Text('$cible${s['rappelJours'] != null ? ' · rappel tous les ${s['rappelJours']} jours' : ''}', style: const TextStyle(color: kMute)),
                if (!fait)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Wrap(spacing: 8, children: [
                      FilledButton(onPressed: () => run(context, ref, () => dio.post('/api/soins/${s['id']}/realise')), child: const Text('Marquer comme fait')),
                      TextButton(onPressed: () => run(context, ref, () => dio.delete('/api/soins/${s['id']}')), child: const Text('Supprimer')),
                    ]),
                  ),
              ]),
            ),
          );
        }),
      OutlinedButton.icon(
        icon: const Icon(Icons.vaccines_outlined),
        label: const Text('Planifier un vaccin ou un traitement'),
        onPressed: () async {
          final r = await showForm(context, 'Planifier un soin', [
            const F('type', 'Type', type: 'select', opts: [['VACCIN', 'Vaccin'], ['TRAITEMENT', 'Traitement']]),
            const F('nom', 'Nom (ex. VHD, myxomatose, vermifuge)'),
            F('lapinId', 'Pour', type: 'select', opts: [['', 'Tout le cheptel'], ...lapinOptions(ls)]),
            const F('datePrevue', 'Date prévue', type: 'date'),
            const F('rappelJours', 'Répéter tous les (jours), facultatif', type: 'number'),
          ]);
          if (r == null || r['nom'] == null || !context.mounted) return;
          final body = {...r};
          if (body['lapinId'] == '' || body['lapinId'] == null) body.remove('lapinId');
          await run(context, ref, () => dio.post('/api/soins', data: body));
        },
      ),
      const Section('Guide des maladies'),
      for (final g in guide)
        Card(
          child: ExpansionTile(
            title: Text('${g['nom']}', style: const TextStyle(fontWeight: FontWeight.w600)),
            childrenPadding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
            expandedCrossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Signes : ${g['signes']}', style: const TextStyle(color: kMute)),
              Bullets('Que faire', g['solutions'] as List),
            ],
          ),
        ),
      const Padding(
        padding: EdgeInsets.only(top: 8),
        child: Text('Conseils généraux : confirmez toujours le diagnostic, le médicament et la dose avec un vétérinaire.', style: TextStyle(fontSize: 13, color: kMute)),
      ),
    ]);
  }
}
