import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';
import 'previsions.dart';
import 'stats.dart';

class HomeScreen extends ConsumerWidget {
  final void Function(String) onGo;
  const HomeScreen({required this.onGo, super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final owner = ref.watch(authProvider.select((a) => a.role == null || a.role == 'PROPRIETAIRE'));
    return view(ref.watch(dashP), (d) {
      final score = (d['score'] as num).toInt();
      final k = Json.from(d['kpis'] as Map);
      final alertes = (d['alertes'] as List).cast<Json>();
      final taches = ref.watch(tachesP).valueOrNull ?? <Json>[];
      final toutes = ref.watch(toutesTachesP).valueOrNull ?? <Json>[];
      final largeur = MediaQuery.of(context).size.width;
      final w = (math.min(largeur - (largeur >= 900 ? 80 : 0), 900.0) - 32 - 16) / 3;
      Widget kpi(String label, String key) => SizedBox(
            width: w,
            child: Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 10),
                child: Column(children: [
                  Text('${k[key]}', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
                  Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 11.5, color: kMute)),
                ]),
              ),
            ),
          );
      return RefreshIndicator(
        onRefresh: () async => ref.read(tickProvider.notifier).state++,
        child: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(color: const Color(0xFF2F6B4F), borderRadius: BorderRadius.circular(22)),
            child: Row(children: [
              SizedBox(
                width: 84, height: 84,
                child: Stack(alignment: Alignment.center, children: [
                  SizedBox.expand(child: CircularProgressIndicator(value: score / 100, strokeWidth: 8, color: Colors.white, backgroundColor: Colors.white24)),
                  Text('$score', style: const TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.w800)),
                ]),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(d['statut'], style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w600)),
                  Text(d['resume'], style: const TextStyle(color: Colors.white70)),
                ]),
              ),
            ]),
          ),
          const SizedBox(height: 12),
          Wrap(spacing: 8, runSpacing: 8, children: [
            kpi('Lapins', 'lapins'),
            kpi('Gestantes', 'gestantes'),
            kpi('Sous la mère', 'sousLaMere'),
            kpi('En engraissement', 'engraissement'),
            kpi('Malades', 'malades'),
            kpi('Cages libres', 'cagesLibres'),
          ]),
          if (owner)
            Card(
            child: ListTile(
              leading: const Icon(Icons.insights),
              title: const Text('Indicateurs de performance'),
              subtitle: const Text('Portées, mortalité, coût par lapin, gain par mois'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const StatsScreen())),
            ),
          ),
          Card(
            child: ListTile(
              leading: const Icon(Icons.event_available),
              title: const Text('Prévisions à 30 jours'),
              subtitle: const Text('Mises bas, sevrages, ventes, stock'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PrevisionsScreen())),
            ),
          ),
          Section('Tâches du jour', hint: '${taches.length}'),
          if (toutes.isEmpty)
            Card(
              child: ListTile(
                leading: const Icon(Icons.checklist),
                title: const Text('Aucune tâche récurrente'),
                subtitle: const Text('Nourrir, nettoyer, peser... ajoutez les tâches habituelles.'),
                trailing: TextButton(
                  onPressed: () => run(context, ref, () => ref.read(dioProvider).post('/api/taches/defauts')),
                  child: const Text('Ajouter'),
                ),
              ),
            )
          else if (taches.isEmpty)
            const Empty('Tout est fait pour aujourd’hui.'),
          for (final t in taches)
            Card(
              child: t['type'] == 'ROUTINE'
                  ? CheckboxListTile(
                      value: false,
                      onChanged: (_) => run(context, ref, () => ref.read(dioProvider).post('/api/taches/${t['id']}/faite')),
                      controlAffinity: ListTileControlAffinity.leading,
                      title: Text('${t['titre']}'),
                      subtitle: Text('${t['detail']}'),
                    )
                  : ListTile(
                      leading: const Icon(Icons.warning_amber_rounded, color: Color(0xFFB97B0B)),
                      title: Text('${t['titre']}'),
                      subtitle: Text('${t['detail']}'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => onGo('${t['section']}'),
                    ),
            ),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton.icon(
              icon: const Icon(Icons.add_task),
              label: const Text('Ajouter une tâche récurrente'),
              onPressed: () async {
                final r = await showForm(context, 'Nouvelle tâche', [
                  const F('titre', 'Tâche'),
                  const F('frequenceJours', 'Répéter tous les (jours)', type: 'number', init: 1),
                ]);
                if (r != null && r['titre'] != null && context.mounted) {
                  await run(context, ref, () => ref.read(dioProvider).post('/api/taches', data: r));
                }
              },
            ),
          ),
          const Section('À traiter maintenant'),
          if (alertes.isEmpty) const Empty('Tout est en ordre.'),
          for (final a in alertes) AlerteCard(a, onGo: onGo),
        ]),
      );
    });
  }
}
