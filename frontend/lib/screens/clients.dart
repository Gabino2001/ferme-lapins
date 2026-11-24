import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';

class ClientsScreen extends ConsumerWidget {
  const ClientsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final clients = ref.watch(clientsP).valueOrNull ?? <Json>[];
    final commandes = ref.watch(commandesP).valueOrNull ?? <Json>[];
    final lots = ref.watch(lotsP).valueOrNull ?? <Json>[];
    final dio = ref.read(dioProvider);

    double total(Json c) => (((c['quantite'] ?? 0) as num) * ((c['prixUnitaire'] ?? 0) as num)).toDouble();
    double reste(Json c) => total(c) - ((c['montantPaye'] ?? 0) as num).toDouble();
    String qui(dynamic id) => nomDe(clients, id).replaceAll('?', 'Client');

    final enCours = commandes.where((c) => c['statut'] == 'EN_ATTENTE').toList()
      ..sort((a, b) => '${a['dateLivraisonPrevue']}'.compareTo('${b['dateLivraisonPrevue']}'));
    final aEncaisser = commandes.where((c) => c['statut'] == 'LIVREE' && reste(c) > 0.5).toList();
    final historique = commandes.where((c) => c['statut'] == 'ANNULEE' || (c['statut'] == 'LIVREE' && reste(c) <= 0.5)).toList()
      ..sort((a, b) => '${b['dateCommande']}'.compareTo('${a['dateCommande']}'));
    final dus = aEncaisser.fold<double>(0, (s, c) => s + reste(c));

    Future<void> paiement(Json c) async {
      final r = await showForm(context, 'Paiement reçu de ${qui(c['clientId'])}', [F('montant', 'Montant (FCFA)', type: 'number', init: reste(c).round())]);
      if (r != null && r['montant'] != null && context.mounted) {
        await run(context, ref, () => dio.post('/api/commandes/${c['id']}/paiement', queryParameters: {'montant': r['montant']}));
      }
    }

    Future<void> livrer(Json c) async {
      final r = await showForm(context, 'Livrer ${qui(c['clientId'])}', [
        F('lotId', 'Retirer les lapins d’un lot', type: 'select', opts: [
          ['', 'Aucun lot (enregistrer seulement la vente)'],
          for (final l in lots) [l['id'].toString(), 'Lot cage ${l['cage'] ?? '-'} · ${l['nombre']} lapereaux'],
        ]),
        const F('date', 'Date de livraison', type: 'date'),
      ]);
      if (r == null || !context.mounted) return;
      final body = {...r};
      if (body['lotId'] == '' || body['lotId'] == null) body.remove('lotId');
      await run(context, ref, () => dio.post('/api/commandes/${c['id']}/livrer', data: body));
    }

    Widget carte(Json c, {bool actions = true}) {
      final statut = '${c['statut']}';
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Expanded(child: Text('${qui(c['clientId'])} · ${c['quantite']} × ${c['produit']}', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
              Pill(statut == 'EN_ATTENTE' ? 'En attente' : statut == 'LIVREE' ? 'Livrée' : 'Annulée',
                  statut == 'EN_ATTENTE' ? const Color(0xFF2F6A9E) : statut == 'LIVREE' ? const Color(0xFF2F6B4F) : Colors.grey),
            ]),
            Text('Total ${fcfa(total(c))} · payé ${fcfa((c['montantPaye'] ?? 0) as num)}${reste(c) > 0.5 && statut != 'ANNULEE' ? ' · reste ${fcfa(reste(c))}' : ''}', style: const TextStyle(color: kMute)),
            if (statut == 'EN_ATTENTE') Text('Livraison prévue le ${dm(c['dateLivraisonPrevue'])}', style: const TextStyle(color: kMute)),
            if (actions)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Wrap(spacing: 8, children: [
                  if (statut == 'EN_ATTENTE') FilledButton(onPressed: () => livrer(c), child: const Text('Livrer')),
                  if (reste(c) > 0.5 && statut != 'ANNULEE') OutlinedButton(onPressed: () => paiement(c), child: const Text('Paiement reçu')),
                  if (statut == 'EN_ATTENTE') TextButton(onPressed: () => run(context, ref, () => dio.post('/api/commandes/${c['id']}/annuler')), child: const Text('Annuler')),
                ]),
              ),
          ]),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Clients et commandes')),
      body: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
        Section('Commandes en cours', hint: '${enCours.length}'),
        if (enCours.isEmpty) const Empty('Aucune commande en attente.'),
        for (final c in enCours) carte(c),
        FilledButton.icon(
          icon: const Icon(Icons.add_shopping_cart),
          label: const Text('Nouvelle commande'),
          onPressed: () async {
            if (clients.isEmpty) {
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Ajoutez d’abord un client.')));
              return;
            }
            final r = await showForm(context, 'Nouvelle commande', [
              F('clientId', 'Client', type: 'select', opts: [for (final c in clients) [c['id'].toString(), '${c['nom']}']]),
              const F('produit', 'Produit', type: 'select', opts: [['Lapin pour la viande', 'Lapin pour la viande'], ['Lapin vivant', 'Lapin vivant'], ['Reproducteur', 'Reproducteur'], ['Autre', 'Autre']]),
              const F('quantite', 'Quantité', type: 'number', init: 1),
              const F('prixUnitaire', 'Prix unitaire (FCFA)', type: 'number'),
              const F('dateLivraisonPrevue', 'Livraison prévue le', type: 'date'),
              const F('montantPaye', 'Acompte déjà versé (FCFA)', type: 'number', init: 0),
            ]);
            if (r == null || !context.mounted) return;
            await run(context, ref, () => dio.post('/api/commandes', data: {...r, 'dateCommande': today(), 'statut': 'EN_ATTENTE', 'montantPaye': r['montantPaye'] ?? 0}));
          },
        ),
        Section('À encaisser', hint: aEncaisser.isEmpty ? '' : fcfa(dus)),
        if (aEncaisser.isEmpty) const Empty('Aucun paiement en attente.'),
        for (final c in aEncaisser) carte(c),
        Section('Clients', hint: '${clients.length}'),
        if (clients.isEmpty) const Empty('Aucun client enregistré.'),
        for (final cl in clients)
          Card(
            child: ListTile(
              leading: const CircleAvatar(child: Icon(Icons.person)),
              title: Text('${cl['nom']}', style: const TextStyle(fontWeight: FontWeight.w600)),
              subtitle: Text('${cl['telephone'] ?? 'Pas de téléphone'}${cl['adresse'] != null ? ' · ${cl['adresse']}' : ''}'),
              trailing: Builder(builder: (_) {
                final du = commandes.where((c) => c['clientId'] == cl['id'] && c['statut'] == 'LIVREE').fold<double>(0, (s, c) => s + (reste(c) > 0 ? reste(c) : 0));
                return du > 0.5 ? Pill('Doit ${fcfa(du)}', const Color(0xFFB97B0B)) : const SizedBox.shrink();
              }),
              onLongPress: () async {
                final ok = await showDialog<bool>(
                  context: context,
                  builder: (_) => AlertDialog(
                    title: Text('Supprimer ${cl['nom']} ?'),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
                      FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Supprimer')),
                    ],
                  ),
                );
                if (ok == true && context.mounted) await run(context, ref, () => dio.delete('/api/clients/${cl['id']}'));
              },
            ),
          ),
        OutlinedButton.icon(
          icon: const Icon(Icons.person_add_alt),
          label: const Text('Nouveau client'),
          onPressed: () async {
            final r = await showForm(context, 'Nouveau client', [const F('nom', 'Nom'), const F('telephone', 'Téléphone'), const F('adresse', 'Adresse ou quartier')]);
            if (r != null && r['nom'] != null && context.mounted) await run(context, ref, () => dio.post('/api/clients', data: r));
          },
        ),
        const Padding(padding: EdgeInsets.only(top: 6), child: Text('Appui long sur un client pour le supprimer.', style: TextStyle(fontSize: 12, color: kMute))),
        if (historique.isNotEmpty) ...[
          const Section('Historique'),
          for (final c in historique.take(5)) carte(c, actions: false),
        ],
      ]),
    );
  }
}
