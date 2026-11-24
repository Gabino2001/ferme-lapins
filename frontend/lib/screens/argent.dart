import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';
import 'package:url_launcher/url_launcher.dart';
import 'clients.dart';

class ArgentScreen extends ConsumerWidget {
  const ArgentScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dio = ref.read(dioProvider);
    final stock = ref.watch(stockP).valueOrNull ?? <Json>[];
    final mvts = [...(ref.watch(mouvementsP).valueOrNull ?? <Json>[])]
      ..sort((a, b) => '${b['date']}'.compareTo('${a['date']}'));

    return view(ref.watch(bilanP), (b) {
      final res = (b['resultat'] as num).toDouble();
      final gain = res >= 0;
      final cr = (b['coutRevient'] as num).toDouble(), pm = (b['prixMoyen'] as num).toDouble();
      final n = (b['lapinsVendus'] as num).toInt();
      final dep = (b['depenses'] as num).toDouble();
      final postes = Map<String, dynamic>.from(b['depensesParPoste'] as Map);

      return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
        Card(
          child: ListTile(
            leading: const Icon(Icons.people_alt_outlined),
            title: const Text('Clients et commandes'),
            subtitle: const Text('Commandes, livraisons, paiements à encaisser'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ClientsScreen())),
          ),
        ),
        Card(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            child: Row(children: [
              const Expanded(child: Text('Exporter les données')),
              TextButton.icon(icon: const Icon(Icons.table_chart_outlined), label: const Text('Excel'), onPressed: () => _export(context, ref, 'excel')),
              TextButton.icon(icon: const Icon(Icons.picture_as_pdf_outlined), label: const Text('PDF'), onPressed: () => _export(context, ref, 'pdf')),
            ]),
          ),
        ),
        const Section('Bilan', hint: '30 derniers jours'),
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(color: gain ? const Color(0xFF2F6B4F) : const Color(0xFFA93A28), borderRadius: BorderRadius.circular(22)),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(gain ? 'Vous gagnez' : 'Vous perdez', style: const TextStyle(color: Colors.white70)),
            Text(fcfa(res.abs()), style: const TextStyle(color: Colors.white, fontSize: 30, fontWeight: FontWeight.w800)),
            Text('Ventes ${fcfa(b['ventes'])} − dépenses ${fcfa(dep)}', style: const TextStyle(color: Colors.white70)),
          ]),
        ),
        const SizedBox(height: 8),
        Row(children: [
          Expanded(child: Card(child: Padding(padding: const EdgeInsets.all(12), child: Column(children: [Text(fcfa(cr), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)), const Text('Coût de revient par lapin vendu', textAlign: TextAlign.center, style: TextStyle(fontSize: 12, color: kMute))])))),
          Expanded(child: Card(child: Padding(padding: const EdgeInsets.all(12), child: Column(children: [Text(fcfa(pm), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)), const Text('Prix de vente moyen', textAlign: TextAlign.center, style: TextStyle(fontSize: 12, color: kMute))])))),
        ]),
        if (n > 0)
          Text(pm >= cr ? 'Chaque lapin vendu rapporte environ ${fcfa(pm - cr)}.' : 'Chaque lapin vendu fait perdre environ ${fcfa(cr - pm)} : vendez plus cher ou réduisez les coûts.',
              style: const TextStyle(color: kMute)),
        const SizedBox(height: 8),
        Wrap(spacing: 8, children: [
          FilledButton(
            onPressed: () async {
              final r = await showForm(context, 'Nouvelle vente', [
                const F('categorie', 'Type', type: 'select', opts: [['Lapin pour la viande', 'Lapin pour la viande'], ['Lapin vivant', 'Lapin vivant'], ['Reproducteur', 'Reproducteur'], ['Autre', 'Autre']]),
                const F('quantite', 'Quantité', type: 'number', init: 1),
                const F('prix', 'Prix unitaire (FCFA)', type: 'number'),
                const F('date', 'Date', type: 'date'),
              ]);
              if (r == null || !context.mounted) return;
              final q = (r['quantite'] ?? 1) as num, p = (r['prix'] ?? 0) as num;
              await run(context, ref, () => dio.post('/api/mouvements', data: {'type': 'VENTE', 'categorie': r['categorie'], 'quantite': q, 'montant': q * p, 'date': r['date']}));
            },
            child: const Text('Enregistrer une vente'),
          ),
          OutlinedButton(
            onPressed: () async {
              final r = await showForm(context, 'Nouvelle dépense', [
                F('categorie', 'Poste', type: 'select', opts: [for (final x in ['Aliment', 'Vétérinaire et médicaments', 'Paille et litière', 'Matériel', 'Main-d’œuvre', 'Autre']) [x, x]]),
                const F('montant', 'Montant (FCFA)', type: 'number'),
                const F('date', 'Date', type: 'date'),
              ]);
              if (r != null && r['montant'] != null && context.mounted) await run(context, ref, () => dio.post('/api/mouvements', data: {...r, 'type': 'DEPENSE'}));
            },
            child: const Text('Enregistrer une dépense'),
          ),
        ]),
        const Section('Dépenses par poste'),
        if (postes.isEmpty) const Empty('Aucune dépense sur la période.'),
        for (final e in postes.entries)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(children: [
                Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                  Text('${e.key} · ${(e.value / dep * 100).round()} %'),
                  Text(fcfa(e.value), style: const TextStyle(fontWeight: FontWeight.w700)),
                ]),
                const SizedBox(height: 6),
                LinearProgressIndicator(value: (e.value / dep).clamp(0, 1).toDouble(), minHeight: 8, borderRadius: BorderRadius.circular(8)),
              ]),
            ),
          ),
        const Section('Derniers mouvements'),
        if (mvts.isEmpty) const Empty('Aucun mouvement.'),
        for (final m in mvts.take(8))
          Card(
            child: ListTile(
              leading: CircleAvatar(child: Text(m['type'] == 'VENTE' ? '+' : '−')),
              title: Text('${m['categorie']}'),
              subtitle: Text('${dm(m['date'])}${m['quantite'] != null && m['type'] == 'VENTE' ? ' · ${m['quantite']} lapin(s)' : ''}'),
              trailing: Text(fcfa(m['montant'] ?? 0), style: const TextStyle(fontWeight: FontWeight.w700)),
            ),
          ),
        Section('Stock', hint: '${stock.length} articles'),
        if (stock.isEmpty) const Empty('Aucun article en stock.'),
        for (final k in stock)
          Builder(builder: (_) {
            final q = ((k['quantite'] ?? 0) as num).toDouble(), conso = ((k['consoJour'] ?? 0) as num).toDouble();
            final j = conso > 0 ? q / conso : 999.0;
            return Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Expanded(child: Text('${k['nom']}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
                    TextButton(
                      onPressed: () async {
                        final r = await showForm(context, 'Réapprovisionner : ${k['nom']}', [F('quantite', 'Quantité ajoutée (${k['unite'] ?? ''})', type: 'number', init: 50)]);
                        if (r != null && context.mounted) await run(context, ref, () => dio.post('/api/stock/${k['id']}/reappro', queryParameters: {'quantite': r['quantite'] ?? 0}));
                      },
                      child: const Text('Réapprovisionner'),
                    ),
                  ]),
                  Text('${k['quantite']} ${k['unite'] ?? ''}${conso > 0 ? ' · environ ${j.floor()} jours' : ''}', style: const TextStyle(color: kMute)),
                  if (conso > 0) ...[
                    const SizedBox(height: 6),
                    LinearProgressIndicator(value: (j / 30).clamp(0, 1).toDouble(), minHeight: 8, color: j < 2 ? Colors.red : j < 5 ? Colors.orange : null, borderRadius: BorderRadius.circular(8)),
                  ],
                ]),
              ),
            );
          }),
        OutlinedButton.icon(
          icon: const Icon(Icons.add),
          label: const Text('Ajouter un article'),
          onPressed: () async {
            final r = await showForm(context, 'Nouvel article de stock', [
              const F('nom', 'Nom (ex. Granulés)'),
              const F('unite', 'Unité (kg, balles, doses...)', init: 'kg'),
              const F('quantite', 'Quantité en stock', type: 'number', init: 0),
              const F('consoJour', 'Consommation par jour', type: 'number', init: 0),
            ]);
            if (r != null && r['nom'] != null && context.mounted) await run(context, ref, () => dio.post('/api/stock', data: r));
          },
        ),
      ]);
    });
  }
}

/// Génère un lien de téléchargement valable 5 minutes et l'ouvre dans le navigateur du téléphone.
Future<void> _export(BuildContext context, WidgetRef ref, String type) async {
  final messenger = ScaffoldMessenger.of(context);
  try {
    final r = await ref.read(dioProvider).post('/api/export/lien', queryParameters: {'type': type});
    final url = Uri.parse('$apiUrl${(r.data as Map)['url']}');
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      messenger.showSnackBar(const SnackBar(content: Text('Impossible d’ouvrir le téléchargement')));
    }
  } catch (e) {
    messenger.showSnackBar(SnackBar(content: Text(errMsg(e))));
  }
}
