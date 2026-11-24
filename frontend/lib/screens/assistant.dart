import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../core.dart';
import '../ui.dart';
import '../voice.dart';

/// Assistant d'élevage (Claude) : questions en texte, à la voix, ou avec une photo.
class AssistantScreen extends ConsumerStatefulWidget {
  const AssistantScreen({super.key});
  @override
  ConsumerState<AssistantScreen> createState() => _AssistantState();
}

class _AssistantState extends ConsumerState<AssistantScreen> {
  final champ = TextEditingController();
  final defil = ScrollController();
  final voice = Voice();
  final List<Map<String, dynamic>> messages = [];
  Uint8List? photo;
  bool attente = false, ecoute = false;

  static const suggestions = [
    'Que dois-je faire aujourd’hui ?',
    'Pourquoi ma mortalité augmente-t-elle ?',
    'Mon élevage est-il rentable ?',
    'Quels lapins vendre en priorité ?',
  ];

  @override
  void dispose() {
    voice.arreter();
    super.dispose();
  }

  void _descendre() => WidgetsBinding.instance.addPostFrameCallback((_) {
        if (defil.hasClients) defil.animateTo(defil.position.maxScrollExtent, duration: const Duration(milliseconds: 250), curve: Curves.easeOut);
      });

  Future<void> envoyer([String? texte]) async {
    final q = (texte ?? champ.text).trim();
    if ((q.isEmpty && photo == null) || attente) return;
    final historique = [
      for (final m in messages.where((m) => m['erreur'] != true).toList().reversed.take(10).toList().reversed) {'role': m['role'], 'content': m['text']},
    ];
    final envoi = photo;
    setState(() {
      messages.add({'role': 'user', 'text': q.isEmpty ? '(photo)' : q, 'photo': envoi});
      attente = true;
      photo = null;
      champ.clear();
    });
    _descendre();
    try {
      final r = await ref.read(dioProvider).post(
            '/api/assistant',
            data: {'question': q, 'historique': historique, if (envoi != null) 'photo': base64Encode(envoi)},
            options: Options(receiveTimeout: const Duration(seconds: 90)),
          );
      final texteRep = (r.data as Map)['reponse'] as String;
      setState(() => messages.add({'role': 'assistant', 'text': texteRep}));
    } catch (e) {
      setState(() => messages.add({'role': 'assistant', 'text': errMsg(e), 'erreur': true}));
    } finally {
      if (mounted) setState(() => attente = false);
      _descendre();
    }
  }

  Future<void> choisirPhoto() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (_) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          if (!kIsWeb) ListTile(leading: const Icon(Icons.photo_camera), title: const Text('Prendre une photo'), onTap: () => Navigator.pop(context, ImageSource.camera)),
          ListTile(leading: const Icon(Icons.photo_library), title: const Text('Choisir dans la galerie'), onTap: () => Navigator.pop(context, ImageSource.gallery)),
        ]),
      ),
    );
    if (source == null) return;
    final x = await ImagePicker().pickImage(source: source, maxWidth: 800, imageQuality: 70);
    if (x == null) return;
    final octets = await x.readAsBytes();
    if (mounted) setState(() => photo = octets);
  }

  Future<void> dicter() async {
    if (ecoute) {
      await voice.arreter();
      setState(() => ecoute = false);
      return;
    }
    final base = champ.text;
    setState(() => ecoute = true);
    final ok = await voice.demarrer(
      (t) => setState(() => champ.text = base.isEmpty ? t : '$base $t'),
      onFin: () {
        if (mounted) setState(() => ecoute = false);
      },
    );
    if (!ok && mounted) {
      setState(() => ecoute = false);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Reconnaissance vocale indisponible sur cet appareil')));
    }
  }

  Widget bulle(Map<String, dynamic> m) {
    final moi = m['role'] == 'user';
    final erreur = m['erreur'] == true;
    final cs = Theme.of(context).colorScheme;
    return Align(
      alignment: moi ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.all(12),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.82),
        decoration: BoxDecoration(
          color: erreur ? const Color(0x40C0402B) : moi ? cs.primaryContainer : cs.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          if (m['photo'] != null)
            Padding(padding: const EdgeInsets.only(bottom: 6), child: ClipRRect(borderRadius: BorderRadius.circular(10), child: Image.memory(m['photo'] as Uint8List, height: 140))),
          SelectableText('${m['text']}'),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Assistant')),
        body: Column(children: [
          Expanded(
            child: ListView(controller: defil, padding: const EdgeInsets.all(16), children: [
              if (messages.isEmpty) ...[
                const Text('Posez une question sur votre ferme : l’assistant connaît vos lapins, vos alertes, votre stock et vos comptes.', style: TextStyle(fontSize: 16)),
                const SizedBox(height: 6),
                const Text('Il donne des pistes, pas un diagnostic : pour une maladie, consultez un vétérinaire. Vos questions et un résumé de la ferme sont envoyés au service d’IA Claude (Anthropic).', style: TextStyle(fontSize: 12, color: kMute)),
                const SizedBox(height: 12),
                Wrap(spacing: 8, runSpacing: 8, children: [for (final s in suggestions) ActionChip(label: Text(s), onPressed: () => envoyer(s))]),
              ],
              for (final m in messages) bulle(m),
              if (attente) const Padding(padding: EdgeInsets.all(12), child: Align(alignment: Alignment.centerLeft, child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 3)))),
            ]),
          ),
          if (photo != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(children: [
                ClipRRect(borderRadius: BorderRadius.circular(8), child: Image.memory(photo!, height: 56)),
                const SizedBox(width: 8),
                const Expanded(child: Text('Photo jointe')),
                IconButton(icon: const Icon(Icons.close), onPressed: () => setState(() => photo = null)),
              ]),
            ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(8, 4, 8, 8),
              child: Row(children: [
                IconButton(tooltip: 'Joindre une photo', icon: const Icon(Icons.photo_camera_outlined), onPressed: attente ? null : choisirPhoto),
                Expanded(
                  child: TextField(
                    controller: champ,
                    minLines: 1,
                    maxLines: 4,
                    textInputAction: TextInputAction.send,
                    onSubmitted: (_) => envoyer(),
                    decoration: const InputDecoration(hintText: 'Votre question...', border: OutlineInputBorder(borderRadius: BorderRadius.all(Radius.circular(24))), contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 10)),
                  ),
                ),
                IconButton(tooltip: 'Dicter', icon: Icon(ecoute ? Icons.mic : Icons.mic_none, color: ecoute ? Colors.red : null), onPressed: attente ? null : dicter),
                IconButton.filled(tooltip: 'Envoyer', icon: const Icon(Icons.send), onPressed: attente ? null : () => envoyer()),
              ]),
            ),
          ),
        ]),
      );
}
