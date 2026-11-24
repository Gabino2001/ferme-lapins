import 'dart:convert';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../genealogie.dart';
import '../qr.dart';
import '../ui.dart';
import 'package:image_picker/image_picker.dart';

class LapinsScreen extends ConsumerWidget {
  const LapinsScreen({super.key});

  Future<void> _add(BuildContext context, WidgetRef ref) async {
    final cages = await cageOptions(ref);
    if (!context.mounted) return;
    final r = await showForm(context, 'Nouveau lapin', [
      const F('nom', 'Nom'),
      const F('sexe', 'Sexe', type: 'select', opts: [['F', 'Femelle'], ['M', 'Mâle']]),
      const F('race', 'Race', init: 'Néo-Zélandais'),
      const F('poids', 'Poids (kg)', type: 'number', init: 4),
      cages.isEmpty ? const F('cage', 'Cage (numéro)') : F('cage', 'Cage', type: 'select', opts: cages),
      F('dateNaissance', 'Date de naissance', type: 'date', init: DateTime.now().subtract(const Duration(days: 180)).toIso8601String().substring(0, 10)),
      F('pereId', 'Père (dans l’élevage)', type: 'select', opts: [['', 'Inconnu ou extérieur'], ...lapinOptions(ref.read(lapinsP).valueOrNull ?? <Json>[], sexe: 'M')]),
      F('mereId', 'Mère (dans l’élevage)', type: 'select', opts: [['', 'Inconnue ou extérieure'], ...lapinOptions(ref.read(lapinsP).valueOrNull ?? <Json>[], sexe: 'F')]),
      const F('pere', 'Père (nom, si extérieur)'),
      const F('mere', 'Mère (nom, si extérieure)'),
    ]);
    if (r == null || r['nom'] == null || !context.mounted) return;
    await run(context, ref, () => ref.read(dioProvider).post('/api/lapins', data: {...r, 'statut': 'ACTIF', 'pereId': r['pereId'] == '' ? null : r['pereId'], 'mereId': r['mereId'] == '' ? null : r['mereId']}));
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
        backgroundColor: Colors.transparent,
        floatingActionButton: FloatingActionButton.extended(
          onPressed: () => _add(context, ref),
          icon: const Icon(Icons.add),
          label: const Text('Lapin'),
        ),
        body: view(ref.watch(lapinsP), (list) {
          if (list.isEmpty) return const Padding(padding: EdgeInsets.all(16), child: Empty('Aucun lapin. Ajoutez le premier avec le bouton +.'));
          return ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 90), children: [
            Section('Lapins', hint: '${list.length} au total · touchez pour la fiche'),
            for (final l in list)
              Card(
                child: ListTile(
                  leading: CircleAvatar(child: Text(l['sexe'] == 'F' ? '♀' : '♂')),
                  title: Text('${l['nom']}', style: const TextStyle(fontWeight: FontWeight.w600)),
                  subtitle: Text('${l['race'] ?? ''} · cage ${l['cage'] ?? '-'} · ${l['poids'] ?? '-'} kg'),
                  trailing: IconButton(
                    icon: const Icon(Icons.delete_outline),
                    tooltip: 'Retirer',
                    onPressed: () async {
                      final ok = await showDialog<bool>(
                        context: context,
                        builder: (_) => AlertDialog(
                          title: Text('Retirer ${l['nom']} ?'),
                          actions: [
                            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Annuler')),
                            FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Retirer')),
                          ],
                        ),
                      );
                      if (ok == true && context.mounted) await run(context, ref, () => ref.read(dioProvider).delete('/api/lapins/${l['id']}'));
                    },
                  ),
                  onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => LapinDetail(l['id'] as int))),
                ),
              ),
          ]);
        }),
      );
}

class LapinDetail extends ConsumerWidget {
  final int id;
  const LapinDetail(this.id, {super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lapins = ref.watch(lapinsP).valueOrNull ?? <Json>[];
    final matches = lapins.where((x) => x['id'] == id);
    if (matches.isEmpty) return Scaffold(appBar: AppBar(), body: const Center(child: CircularProgressIndicator()));
    final l = matches.first;
    final pesees = (ref.watch(peseesP(id)).valueOrNull ?? <Json>[]);
    final portees = (ref.watch(porteesP).valueOrNull ?? <Json>[]).where((p) => p['mereId'] == id).toList();
    final saillies = (ref.watch(sailliesP).valueOrNull ?? <Json>[]).where((s) => s['femelleId'] == id || s['maleId'] == id).toList();
    final cas = (ref.watch(casP).valueOrNull ?? <Json>[]).where((c) => c['lapinId'] == id).toList();
    final age = daysSince(l['dateNaissance']);
    final last = pesees.length > 6 ? pesees.sublist(pesees.length - 6) : pesees;
    final mx = last.fold<double>(0.1, (m, p) => (p['poids'] as num) > m ? (p['poids'] as num).toDouble() : m);
    final nes = portees.fold<int>(0, (n, p) => n + ((p['nes'] ?? 0) as int));
    final sev = portees.fold<int>(0, (n, p) => n + ((p['sevres'] ?? 0) as int));
    final femelle = l['sexe'] == 'F';

    return Scaffold(
      appBar: AppBar(
        title: Text('${femelle ? '♀' : '♂'} ${l['nom']}'),
        actions: [IconButton(tooltip: 'Code QR', icon: const Icon(Icons.qr_code_2), onPressed: () => showQr(context, '${l['nom']}', 'ferme:lapin:$id'))],
      ),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Center(child: _Photo(id)),
        const SizedBox(height: 8),
        Text('${l['race'] ?? ''} · cage ${l['cage'] ?? '-'} · ${age >= 60 ? '${age ~/ 30} mois' : '$age j'}', style: const TextStyle(color: kMute)),
        Section('Poids', hint: '${l['poids'] ?? '-'} kg'),
        if (last.isEmpty) const Empty('Aucune pesée enregistrée.'),
        Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          for (final p in last)
            Expanded(
              child: Column(children: [
                Container(height: (p['poids'] as num) / mx * 60, margin: const EdgeInsets.symmetric(horizontal: 4), decoration: BoxDecoration(color: const Color(0xFF2F6B4F), borderRadius: BorderRadius.circular(6))),
                Text('${p['poids']}', style: const TextStyle(fontSize: 11)),
                Text(dm(p['date']), style: const TextStyle(fontSize: 10, color: kMute)),
              ]),
            ),
        ]),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            icon: const Icon(Icons.monitor_weight_outlined),
            label: const Text('Ajouter une pesée'),
            onPressed: () async {
              final r = await showForm(context, 'Nouvelle pesée', [const F('poids', 'Poids (kg)', type: 'number'), const F('date', 'Date', type: 'date')]);
              if (r != null && r['poids'] != null && context.mounted) await run(context, ref, () => ref.read(dioProvider).post('/api/lapins/$id/pesees', data: r));
            },
          ),
        ),
        const Section('Généalogie'),
        ArbreGenealogique(id),
        const Section('Reproduction'),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(femelle ? '${portees.length} portée(s) · $nes nés · $sev sevrés' : 'Mâle reproducteur'),
              for (final p in portees) Text('Portée du ${dm(p['date'])} : ${p['nes']} nés, ${p['vivants']} vivants', style: const TextStyle(color: kMute)),
              for (final s in saillies) Text('Saillie du ${dm(s['date'])} · ${nomDe(lapins, s['femelleId'])} × ${nomDe(lapins, s['maleId'])}${s['palpation'] == 'ok' ? ' · gestante' : ''}', style: const TextStyle(color: kMute)),
            ]),
          ),
        ),
        const Section('Santé'),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: cas.isEmpty
                ? const Text('Aucun problème enregistré.')
                : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [for (final c in cas) Text('${c['maladie']} · ${dm(c['date'])} · ${c['statut']}')]),
          ),
        ),
        const Section('Notes'),
        Card(child: Padding(padding: const EdgeInsets.all(12), child: Text('${l['notes'] ?? 'Aucune note.'}'))),
        const SizedBox(height: 12),
        FilledButton.icon(
          icon: const Icon(Icons.edit_outlined),
          label: const Text('Modifier la fiche'),
          onPressed: () async {
            final cages = await cageOptions(ref);
            if (!context.mounted) return;
            final r = await showForm(context, 'Modifier ${l['nom']}', [
              cages.isEmpty ? F('cage', 'Cage', init: l['cage']) : F('cage', 'Cage', type: 'select', opts: cages, init: l['cage']),
              F('pereId', 'Père (dans l’élevage)', type: 'select', opts: [['', 'Inconnu ou extérieur'], for (final o in lapinOptions(lapins, sexe: 'M')) if (o[0] != '$id') o], init: l['pereId']?.toString() ?? ''),
              F('mereId', 'Mère (dans l’élevage)', type: 'select', opts: [['', 'Inconnue ou extérieure'], for (final o in lapinOptions(lapins, sexe: 'F')) if (o[0] != '$id') o], init: l['mereId']?.toString() ?? ''),
              F('pere', 'Père (nom, si extérieur)', init: l['pere']),
              F('mere', 'Mère (nom, si extérieure)', init: l['mere']),
              F('notes', 'Notes', init: l['notes']),
            ]);
            if (r != null && context.mounted) {
              await run(context, ref, () => ref.read(dioProvider).put('/api/lapins/$id', data: {...l, ...r, 'pereId': r['pereId'] == '' ? null : r['pereId'], 'mereId': r['mereId'] == '' ? null : r['mereId']}));
            }
          },
        ),
      ]),
    );
  }
}

class _Photo extends ConsumerWidget {
  final int id;
  const _Photo(this.id);

  Future<void> _choisir(BuildContext context, WidgetRef ref) async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (_) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          if (!kIsWeb)
            ListTile(leading: const Icon(Icons.photo_camera), title: const Text('Prendre une photo'), onTap: () => Navigator.pop(context, ImageSource.camera)),
          ListTile(leading: const Icon(Icons.photo_library), title: const Text('Choisir dans la galerie'), onTap: () => Navigator.pop(context, ImageSource.gallery)),
        ]),
      ),
    );
    if (source == null) return;
    final x = await ImagePicker().pickImage(source: source, maxWidth: 640, imageQuality: 70);
    if (x == null || !context.mounted) return;
    final bytes = await x.readAsBytes();
    if (!context.mounted) return;
    await run(context, ref, () => ref.read(dioProvider).put('/api/lapins/$id/photo', data: {'photo': base64Encode(bytes)}));
    ref.invalidate(photoP(id));
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final photo = ref.watch(photoP(id)).valueOrNull;
    return GestureDetector(
      onTap: () => _choisir(context, ref),
      child: Stack(alignment: Alignment.bottomRight, children: [
        CircleAvatar(
          radius: 56,
          backgroundImage: photo != null ? MemoryImage(photo) : null,
          child: photo == null ? const Icon(Icons.pets, size: 40) : null,
        ),
        const CircleAvatar(radius: 16, child: Icon(Icons.photo_camera, size: 16)),
      ]),
    );
  }
}
