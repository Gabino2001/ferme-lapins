import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core.dart';
import 'voice.dart';

/// Gris des textes secondaires et des filets, lisibles en thème clair comme en thème sombre.
const kMute = Color(0xFF6B7A71);
const kLine = Color(0x4D808080);

Widget view<T>(AsyncValue<T> v, Widget Function(T) build) => v.when(
      skipLoadingOnReload: true,
      data: build,
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(errMsg(e), textAlign: TextAlign.center))),
    );

class Section extends StatelessWidget {
  final String title;
  final String? hint;
  const Section(this.title, {this.hint, super.key});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 20, bottom: 8),
        child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          if (hint != null) Text(hint!, style: const TextStyle(fontSize: 13, color: kMute)),
        ]),
      );
}

class Empty extends StatelessWidget {
  final String text;
  const Empty(this.text, {super.key});
  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        margin: const EdgeInsets.only(bottom: 8),
        decoration: BoxDecoration(border: Border.all(color: kLine), borderRadius: BorderRadius.circular(14)),
        child: Text(text, textAlign: TextAlign.center, style: const TextStyle(color: kMute)),
      );
}

class Pill extends StatelessWidget {
  final String text;
  final Color color;
  const Pill(this.text, this.color, {super.key});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 2),
        decoration: BoxDecoration(color: color.withOpacity(.14), borderRadius: BorderRadius.circular(99)),
        child: Text(text, style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: color)),
      );
}

class Bullets extends StatelessWidget {
  final String title;
  final List<dynamic> items;
  const Bullets(this.title, this.items, {super.key});
  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        margin: const EdgeInsets.only(top: 8),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(color: Theme.of(context).colorScheme.surfaceContainerHighest.withOpacity(.5), borderRadius: BorderRadius.circular(10)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
          for (final s in items) Text('•  $s'),
        ]),
      );
}

class AlerteCard extends StatelessWidget {
  final Json a;
  final void Function(String)? onGo;
  const AlerteCard(this.a, {this.onGo, super.key});
  @override
  Widget build(BuildContext context) {
    final c = sevColor(a['severite']);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Container(
        decoration: BoxDecoration(border: Border(left: BorderSide(color: c, width: 5))),
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Expanded(child: Text(a['titre'], style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))),
            Pill(a['tag'], c),
          ]),
          Text(a['detail'] ?? '', style: const TextStyle(color: kMute)),
          Bullets('Solution conseillée', a['solutions'] as List),
          if (onGo != null)
            Align(alignment: Alignment.centerLeft, child: TextButton(onPressed: () => onGo!(a['section']), child: const Text('Ouvrir la section'))),
        ]),
      ),
    );
  }
}

// ---------------------------------------------------------------- Formulaire générique
class F {
  final String k, label, type; // type : text | number | date | select
  final List<List<String>> opts; // [valeur, libellé]
  final Object? init;
  final bool obscure;
  const F(this.k, this.label, {this.type = 'text', this.opts = const [], this.init, this.obscure = false});
}

Future<Json?> showForm(BuildContext context, String title, List<F> fields) => showModalBottomSheet<Json>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => _FormSheet(title, fields),
    );

class _FormSheet extends StatefulWidget {
  final String title;
  final List<F> fields;
  const _FormSheet(this.title, this.fields);
  @override
  State<_FormSheet> createState() => _FormSheetState();
}

class _FormSheetState extends State<_FormSheet> {
  final Voice voice = Voice();
  String? dicte;
  late final Map<String, TextEditingController> ctl = {
    for (final f in widget.fields) f.k: TextEditingController(text: f.init?.toString() ?? (f.type == 'date' ? today() : '')),
  };
  late final Map<String, String?> sel = {
    for (final f in widget.fields.where((f) => f.type == 'select'))
      f.k: f.init?.toString() ?? (f.opts.isNotEmpty ? f.opts.first[0] : null),
  };

  @override
  void dispose() {
    voice.arreter();
    super.dispose();
  }

  Widget field(F f) {
    if (f.type == 'select') {
      return DropdownButtonFormField<String>(
        initialValue: sel[f.k],
        isExpanded: true,
        decoration: InputDecoration(labelText: f.label),
        items: [for (final o in f.opts) DropdownMenuItem(value: o[0], child: Text(o[1], overflow: TextOverflow.ellipsis))],
        onChanged: (v) => sel[f.k] = v,
      );
    }
    if (f.type == 'date') {
      return TextField(
        controller: ctl[f.k],
        readOnly: true,
        decoration: InputDecoration(labelText: f.label, suffixIcon: const Icon(Icons.calendar_today)),
        onTap: () async {
          final d = await showDatePicker(
            context: context,
            initialDate: DateTime.tryParse(ctl[f.k]!.text) ?? DateTime.now(),
            firstDate: DateTime(2015),
            lastDate: DateTime.now().add(const Duration(days: 730)),
          );
          if (d != null) ctl[f.k]!.text = d.toIso8601String().substring(0, 10);
        },
      );
    }
    return TextField(
      controller: ctl[f.k],
      obscureText: f.obscure,
      keyboardType: f.type == 'number' ? const TextInputType.numberWithOptions(decimal: true) : TextInputType.text,
      decoration: InputDecoration(
        labelText: f.label,
        suffixIcon: (f.type == 'text' && !f.obscure)
            ? IconButton(
                tooltip: 'Dicter',
                icon: Icon(dicte == f.k ? Icons.mic : Icons.mic_none, color: dicte == f.k ? Colors.red : null),
                onPressed: () async {
                  if (dicte == f.k) {
                    await voice.arreter();
                    if (mounted) setState(() => dicte = null);
                    return;
                  }
                  final base = ctl[f.k]!.text;
                  setState(() => dicte = f.k);
                  final ok = await voice.demarrer(
                    (t) => setState(() => ctl[f.k]!.text = base.isEmpty ? t : '$base $t'),
                    onFin: () {
                      if (mounted) setState(() => dicte = null);
                    },
                  );
                  if (!ok && mounted) {
                    setState(() => dicte = null);
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Reconnaissance vocale indisponible sur cet appareil')));
                  }
                },
              )
            : null,
      ),
    );
  }

  void save() {
    final out = <String, dynamic>{};
    for (final f in widget.fields) {
      if (f.type == 'select') {
        final v = sel[f.k];
        out[f.k] = (f.k.endsWith('Id') && v != null) ? (int.tryParse(v) ?? v) : v;
      } else {
        final t = ctl[f.k]!.text.trim();
        if (t.isEmpty) continue;
        if (f.type == 'number') {
          final n = double.tryParse(t.replaceAll(',', '.'));
          if (n != null) out[f.k] = n == n.roundToDouble() ? n.toInt() : n;
        } else {
          out[f.k] = t;
        }
      }
    }
    Navigator.pop(context, out);
  }

  @override
  Widget build(BuildContext context) => Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
        child: SingleChildScrollView(
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(widget.title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            for (final f in widget.fields) Padding(padding: const EdgeInsets.only(bottom: 10), child: field(f)),
            Row(children: [
              Expanded(child: OutlinedButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler'))),
              const SizedBox(width: 10),
              Expanded(child: FilledButton(onPressed: save, child: const Text('Enregistrer'))),
            ]),
          ]),
        ),
      );
}

/// Options de cage (numéros) pour les formulaires.
Future<List<List<String>>> cageOptions(WidgetRef ref) async {
  final cages = await ref.read(cagesP.future);
  return [for (final c in cages) [c['numero'].toString(), 'Cage ${c['numero']} (${c['occupation']}/${c['capacite']})']];
}

List<List<String>> lapinOptions(List<Json> ls, {String? sexe}) => [
      for (final l in ls.where((l) => sexe == null || l['sexe'] == sexe)) [l['id'].toString(), '${l['nom']} · cage ${l['cage'] ?? '-'}'],
    ];
