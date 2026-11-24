import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core.dart';
import 'ui.dart';

final genealogieP = FutureProvider.family<Json, int>((ref, id) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).obj('/api/genealogie/$id');
});

/// Vérifie le lien de parenté avant une saillie. Retourne true si on peut continuer.
Future<bool> confirmerParente(BuildContext context, Dio dio, dynamic femelleId, dynamic maleId) async {
  Json p;
  try {
    p = await dio.obj('/api/genealogie/parente?femelleId=$femelleId&maleId=$maleId');
  } catch (_) {
    return true; // hors-ligne ou erreur : on ne bloque pas la saisie
  }
  final niveau = '${p['niveau']}';
  if (niveau == 'AUCUN' || !context.mounted) return true;
  final eleve = niveau == 'ELEVE';
  final ok = await showDialog<bool>(
    context: context,
    builder: (_) => AlertDialog(
      icon: Icon(Icons.warning_amber_rounded, color: eleve ? Colors.red : Colors.orange),
      title: Text(eleve ? 'Consanguinité élevée' : niveau == 'MOYEN' ? 'Consanguinité moyenne' : 'Parenté éloignée'),
      content: Text('${p['message']}${eleve ? '\n\nDéconseillé : risque de petites portées et de lapereaux fragiles.' : ''}'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
        FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Enregistrer quand même')),
      ],
    ),
  );
  return ok == true;
}

/// Ascendance (3 générations) et descendance d'un lapin.
class ArbreGenealogique extends ConsumerWidget {
  final int id;
  const ArbreGenealogique(this.id, {super.key});

  Widget _noeud(Json? n, String role, int niveau) {
    if (n == null) return const SizedBox.shrink();
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Padding(
        padding: EdgeInsets.only(left: niveau * 18.0, top: 3, bottom: 3),
        child: Text('${niveau == 0 ? '' : '└ '}$role : ${n['nom']}${n['id'] == null ? ' (hors élevage)' : ''}'),
      ),
      _noeud(n['pere'] as Json?, 'Père', niveau + 1),
      _noeud(n['mere'] as Json?, 'Mère', niveau + 1),
    ]);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final g = ref.watch(genealogieP(id)).valueOrNull;
    if (g == null) return const Card(child: Padding(padding: EdgeInsets.all(12), child: Text('Chargement...')));
    final racine = g['ascendance'] as Json?;
    final pere = racine?['pere'] as Json?, mere = racine?['mere'] as Json?;
    final enfants = (g['enfants'] as List).cast<Json>();
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          if (pere == null && mere == null)
            const Text('Parents non renseignés : touchez « Modifier la fiche » pour les indiquer.', style: TextStyle(color: kMute)),
          _noeud(pere, 'Père', 0),
          _noeud(mere, 'Mère', 0),
          if (enfants.isNotEmpty) ...[
            const Divider(),
            Text('Descendants : ${enfants.map((e) => e['nom']).join(', ')}'),
          ],
        ]),
      ),
    );
  }
}
