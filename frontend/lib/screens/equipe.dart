import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';

class EquipeScreen extends ConsumerWidget {
  const EquipeScreen({super.key});

  static const _roles = {'OUVRIER': 'Ouvrier', 'LECTEUR': 'Lecteur'};

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final dio = ref.read(dioProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Équipe et droits')),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.person_add),
        label: const Text('Ajouter'),
        onPressed: () async {
          final r = await showForm(context, 'Nouveau membre', [
            const F('nom', 'Nom'),
            const F('email', 'Email (sert à se connecter)'),
            const F('motDePasse', 'Mot de passe provisoire (6 caractères minimum)', obscure: true),
            const F('role', 'Rôle', type: 'select', opts: [['OUVRIER', 'Ouvrier'], ['LECTEUR', 'Lecteur']]),
          ]);
          if (r != null && r['nom'] != null && r['email'] != null && context.mounted) {
            await run(context, ref, () => dio.post('/api/equipe', data: r));
            ref.invalidate(equipeP);
          }
        },
      ),
      body: view(ref.watch(equipeP), (membres) => ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 90), children: [
            const Card(
              child: Padding(
                padding: EdgeInsets.all(12),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('Droits des rôles', style: TextStyle(fontWeight: FontWeight.w600)),
                  SizedBox(height: 4),
                  Text('• Propriétaire : tout, y compris l’argent, les clients et l’équipe.'),
                  Text('• Ouvrier : tout le travail de la ferme (lapins, cages, élevage, santé, tâches), sans l’argent, les clients ni l’équipe.'),
                  Text('• Lecteur : consultation seulement, aucune modification.'),
                ]),
              ),
            ),
            Section('Membres', hint: '${membres.length}'),
            if (membres.isEmpty) const Empty('Aucun membre. Ajoutez un ouvrier ou un lecteur avec le bouton Ajouter.'),
            for (final m in membres)
              Card(
                child: ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.person)),
                  title: Text('${m['nom']}', style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text('${m['email']}'),
                  trailing: PopupMenuButton<String>(
                    icon: Pill(_roles['${m['role']}'] ?? '${m['role']}', const Color(0xFF2F6A9E)),
                    onSelected: (v) async {
                      if (v == 'retirer') {
                        final ok = await showDialog<bool>(
                          context: context,
                          builder: (_) => AlertDialog(
                            title: Text('Retirer ${m['nom']} ?'),
                            content: const Text('Cette personne ne pourra plus se connecter.'),
                            actions: [
                              TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
                              FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Retirer')),
                            ],
                          ),
                        );
                        if (ok == true && context.mounted) {
                          await run(context, ref, () => dio.delete('/api/equipe/${m['id']}'));
                          ref.invalidate(equipeP);
                        }
                      } else if (v == 'mdp') {
                        final r = await showForm(context, 'Nouveau mot de passe de ${m['nom']}', [const F('nouveau', 'Mot de passe (6 caractères minimum)', obscure: true)]);
                        if (r != null && r['nouveau'] != null && context.mounted) {
                          await run(context, ref, () => dio.put('/api/equipe/${m['id']}/mot-de-passe', data: r));
                        }
                      } else {
                        await run(context, ref, () => dio.put('/api/equipe/${m['id']}/role', queryParameters: {'role': v}));
                        ref.invalidate(equipeP);
                      }
                    },
                    itemBuilder: (_) => const [
                      PopupMenuItem(value: 'OUVRIER', child: Text('Passer en ouvrier')),
                      PopupMenuItem(value: 'LECTEUR', child: Text('Passer en lecteur')),
                      PopupMenuItem(value: 'mdp', child: Text('Nouveau mot de passe')),
                      PopupMenuItem(value: 'retirer', child: Text('Retirer de l’équipe')),
                    ],
                  ),
                ),
              ),
          ])),
    );
  }
}
