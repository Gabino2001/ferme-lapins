import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../genealogie.dart';
import '../ui.dart';
import 'croissance.dart';
import 'planning.dart';

class ElevageScreen extends ConsumerWidget {
  const ElevageScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ls = ref.watch(lapinsP).valueOrNull ?? <Json>[];
    final ss = ref.watch(sailliesP).valueOrNull ?? <Json>[];
    final ps = ref.watch(porteesP).valueOrNull ?? <Json>[];
    final lots = ref.watch(lotsP).valueOrNull ?? <Json>[];
    final dio = ref.read(dioProvider);
    final total = lots.fold<int>(0, (n, l) => n + ((l['nombre'] ?? 0) as int));

    Widget btn(String t, VoidCallback f, {bool ok = false}) =>
        ok ? FilledButton(onPressed: f, child: Text(t)) : OutlinedButton(onPressed: f, child: Text(t));

    return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
      Card(
        child: ListTile(
          leading: const Icon(Icons.event_note),
          title: const Text('Planning de reproduction'),
          subtitle: const Text('Qui saillir, quand, et avec quel mâle'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PlanningScreen())),
        ),
      ),
      const Section('Saillies en cours'),
      if (ss.isEmpty) const Empty('Aucune saillie en cours.'),
      for (final s in ss)
        Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Expanded(child: Text('${nomDe(ls, s['femelleId'])} × ${nomDe(ls, s['maleId'])}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
                Pill(s['palpation'] == 'ok' ? 'Gestante' : s['palpation'] == 'non' ? 'Vide' : 'À palper', const Color(0xFF2F6B4F)),
              ]),
              Text('Saillie le ${dm(s['date'])} · mise bas prévue le ${dm(DateTime.parse(s['date']).add(const Duration(days: 31)).toIso8601String())}',
                  style: const TextStyle(color: kMute)),
              const SizedBox(height: 8),
              Wrap(spacing: 8, children: [
                if (s['palpation'] == null) ...[
                  btn('Palpation positive', () => run(context, ref, () => dio.post('/api/saillies/${s['id']}/palpation', queryParameters: {'resultat': 'ok'})), ok: true),
                  btn('Négative', () => run(context, ref, () => dio.post('/api/saillies/${s['id']}/palpation', queryParameters: {'resultat': 'non'}))),
                ],
                if (s['palpation'] != 'non')
                  btn('Enregistrer la mise bas', () async {
                    final r = await showForm(context, 'Mise bas de ${nomDe(ls, s['femelleId'])}', [
                      const F('date', 'Date', type: 'date'),
                      const F('nes', 'Nés', type: 'number', init: 8),
                      const F('vivants', 'Vivants', type: 'number', init: 8),
                    ]);
                    if (r != null && context.mounted) await run(context, ref, () => dio.post('/api/saillies/${s['id']}/mise-bas', data: r));
                  }, ok: true)
                else
                  btn('Clôturer', () => run(context, ref, () => dio.delete('/api/saillies/${s['id']}'))),
              ]),
            ]),
          ),
        ),
      OutlinedButton.icon(
        icon: const Icon(Icons.add),
        label: const Text('Enregistrer une saillie'),
        onPressed: () async {
          final r = await showForm(context, 'Nouvelle saillie', [
            F('femelleId', 'Femelle', type: 'select', opts: lapinOptions(ls, sexe: 'F')),
            F('maleId', 'Mâle', type: 'select', opts: lapinOptions(ls, sexe: 'M')),
            const F('date', 'Date', type: 'date'),
          ]);
          if (r != null && r['femelleId'] != null && r['maleId'] != null && context.mounted) {
            if (!await confirmerParente(context, dio, r['femelleId'], r['maleId'])) return;
            if (context.mounted) await run(context, ref, () => dio.post('/api/saillies', data: r));
          }
        },
      ),
      const Section('Portées'),
      if (ps.isEmpty) const Empty('Aucune portée.'),
      for (final p in ps)
        Builder(builder: (_) {
          final du = 35 - daysSince(p['date']);
          final fait = p['sevree'] == true;
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Expanded(child: Text('Portée de ${nomDe(ls, p['mereId'])}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
                  Pill(fait ? '${p['sevres']} sevrés' : du > 0 ? 'Sevrage dans $du j' : 'À sevrer', du <= 0 && !fait ? const Color(0xFFB97B0B) : const Color(0xFF2F6B4F)),
                ]),
                Text('Née le ${dm(p['date'])} · ${p['nes']} nés, ${p['vivants']} vivants', style: const TextStyle(color: kMute)),
                if (!fait)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: btn('Enregistrer le sevrage', () async {
                      final cages = await cageOptions(ref);
                      if (!context.mounted) return;
                      final r = await showForm(context, 'Sevrage', [
                        F('nombre', 'Lapereaux sevrés', type: 'number', init: p['vivants']),
                        cages.isEmpty ? const F('cage', 'Cage d’engraissement') : F('cage', 'Cage d’engraissement', type: 'select', opts: cages),
                      ]);
                      if (r != null && context.mounted) await run(context, ref, () => dio.post('/api/portees/${p['id']}/sevrage', data: {'nombre': r['nombre'] ?? 0, 'cage': r['cage']}));
                    }, ok: true),
                  ),
              ]),
            ),
          );
        }),
      OutlinedButton.icon(
        icon: const Icon(Icons.add),
        label: const Text('Ajouter une portée'),
        onPressed: () async {
          final r = await showForm(context, 'Nouvelle portée', [
            F('mereId', 'Mère', type: 'select', opts: lapinOptions(ls, sexe: 'F')),
            const F('date', 'Date de naissance', type: 'date'),
            const F('nes', 'Nés', type: 'number', init: 8),
            const F('vivants', 'Vivants', type: 'number', init: 8),
          ]);
          if (r != null && r['mereId'] != null && context.mounted) await run(context, ref, () => dio.post('/api/portees', data: {...r, 'sevres': 0, 'sevree': false}));
        },
      ),
      Section('Engraissement', hint: '$total lapereaux'),
      if (lots.isEmpty) const Empty('Aucun lot en engraissement.'),
      for (final l in lots)
        Builder(builder: (_) {
          final age = daysSince(l['dateNaissance']);
          final pret = age >= 70;
          return Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(children: [
                  Expanded(child: Text('${l['nombre']} lapereaux · cage ${l['cage'] ?? '-'}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
                  Pill(pret ? 'Prêt à vendre' : 'Dans ${70 - age} j', pret ? const Color(0xFF2F6B4F) : const Color(0xFF2F6A9E)),
                ]),
                Text('Portée de ${nomDe(ls, l['mereId'])} · ${age ~/ 7} semaines', style: const TextStyle(color: kMute)),
                const SizedBox(height: 6),
                LinearProgressIndicator(value: (age / 70).clamp(0, 1).toDouble(), minHeight: 8, borderRadius: BorderRadius.circular(8)),
                const SizedBox(height: 8),
                Wrap(spacing: 8, children: [
                  btn('Vendre', () async {
                    final r = await showForm(context, 'Vendre des lapereaux', [
                      F('quantite', 'Quantité', type: 'number', init: l['nombre']),
                      const F('prixUnitaire', 'Prix unitaire (FCFA)', type: 'number'),
                      const F('date', 'Date', type: 'date'),
                    ]);
                    if (r != null && context.mounted) {
                      await run(context, ref, () => dio.post('/api/lots/${l['id']}/vente', data: {'quantite': r['quantite'] ?? 0, 'prixUnitaire': r['prixUnitaire'] ?? 0, 'date': r['date']}));
                    }
                  }, ok: true),
                  btn('Peser', () async {
                    final r = await showForm(context, 'Pesée du lot', [
                      const F('poids', 'Poids moyen d’un lapin (kg)', type: 'number'),
                      const F('date', 'Date', type: 'date'),
                    ]);
                    if (r != null && r['poids'] != null && context.mounted) await run(context, ref, () => dio.post('/api/lots/${l['id']}/pesees', data: r));
                  }),
                  btn('Courbe', () => Navigator.push(context, MaterialPageRoute(builder: (_) => CroissanceScreen(l['id'] as int)))),
                  btn('Perte', () async {
                    final r = await showForm(context, 'Lapereaux morts', [const F('nombre', 'Nombre', type: 'number', init: 1)]);
                    if (r != null && context.mounted) await run(context, ref, () => dio.post('/api/lots/${l['id']}/perte', data: {'nombre': r['nombre'] ?? 0}));
                  }),
                ]),
              ]),
            ),
          );
        }),
      const Padding(
        padding: EdgeInsets.only(top: 8),
        child: Text('Prêt à vendre à partir de 10 semaines (70 jours) : repère général à ajuster selon votre race.', style: TextStyle(fontSize: 13, color: kMute)),
      ),
    ]);
  }
}
