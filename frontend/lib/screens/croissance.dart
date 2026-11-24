import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';

final croissanceP = FutureProvider.family<Json, int>((ref, id) {
  ref.watch(tickProvider);
  return ref.watch(dioProvider).obj('/api/lots/$id/pesees');
});

/// Courbe de croissance d'un lot d'engraissement (poids moyen d'un lapin au fil des pesées).
class CroissanceScreen extends ConsumerWidget {
  final int lotId;
  const CroissanceScreen(this.lotId, {super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
        appBar: AppBar(title: const Text('Croissance du lot')),
        floatingActionButton: FloatingActionButton.extended(
          icon: const Icon(Icons.monitor_weight_outlined),
          label: const Text('Peser'),
          onPressed: () async {
            final r = await showForm(context, 'Pesée du lot', [
              const F('poids', 'Poids moyen d’un lapin (kg)', type: 'number'),
              const F('date', 'Date', type: 'date'),
            ]);
            if (r != null && r['poids'] != null && context.mounted) {
              await run(context, ref, () => ref.read(dioProvider).post('/api/lots/$lotId/pesees', data: r));
            }
          },
        ),
        body: view(ref.watch(croissanceP(lotId)), (c) {
          final points = (c['points'] as List).cast<Json>();
          final parJour = c['gainParJour'] as num?;
          final derniere = c['gainDernierePeriode'] as num?;
          final conseil = c['conseil'] as String?;
          Widget tuile(String valeur, String label) => Expanded(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(children: [
                      Text(valeur, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                      Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 12, color: kMute)),
                    ]),
                  ),
                ),
              );
          return ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 90), children: [
            if (points.length < 2)
              const Empty('Ajoutez au moins deux pesées (bouton Peser) pour voir la courbe de croissance.')
            else
              Card(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(8, 12, 8, 8),
                  child: SizedBox(
                    height: 220,
                    child: CustomPaint(
                      size: Size.infinite,
                      painter: _Courbe([for (final p in points) (p['poids'] as num).toDouble()], Theme.of(context).colorScheme.primary, Theme.of(context).colorScheme.onSurface),
                    ),
                  ),
                ),
              ),
            if (parJour != null || derniere != null)
              Row(children: [
                if (parJour != null) tuile('$parJour g/j', 'Gain moyen par jour'),
                if (derniere != null) tuile('$derniere g/j', 'Gain depuis la dernière pesée'),
              ]),
            if (conseil != null)
              Card(
                color: const Color(0x40B97B0B),
                child: Padding(padding: const EdgeInsets.all(12), child: Text(conseil)),
              ),
            const Section('Pesées'),
            if (points.isEmpty) const Empty('Aucune pesée enregistrée.'),
            for (final p in points.reversed)
              Card(
                child: ListTile(
                  leading: const Icon(Icons.monitor_weight_outlined),
                  title: Text('${p['poids']} kg par lapin'),
                  subtitle: Text('${dm(p['date'])}${p['age'] != null ? ' · ${p['age']} jours d’âge' : ''}'),
                ),
              ),
            const Padding(
              padding: EdgeInsets.only(top: 8),
              child: Text('Pesez quelques lapins du lot et saisissez leur poids moyen. Repère général : à vous de décider du meilleur moment de vente selon vos clients.', style: TextStyle(fontSize: 12, color: kMute)),
            ),
          ]);
        }),
      );
}

class _Courbe extends CustomPainter {
  final List<double> y;
  final Color couleur, texte;
  _Courbe(this.y, this.couleur, this.texte);

  @override
  void paint(Canvas canvas, Size size) {
    if (y.length < 2) return;
    final mn = y.reduce(math.min), mx = y.reduce(math.max);
    const marge = 28.0;
    final h = size.height - 2 * marge, w = size.width - 2 * marge;
    final etendue = (mx - mn) == 0 ? 1.0 : mx - mn;
    Offset pt(int i) => Offset(marge + w * i / (y.length - 1), marge + h * (1 - (y[i] - mn) / etendue));
    final ligne = Paint()
      ..color = couleur
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke;
    final chemin = Path()..moveTo(pt(0).dx, pt(0).dy);
    for (var i = 1; i < y.length; i++) {
      chemin.lineTo(pt(i).dx, pt(i).dy);
    }
    canvas.drawPath(chemin, ligne);
    final rond = Paint()..color = couleur;
    for (var i = 0; i < y.length; i++) {
      canvas.drawCircle(pt(i), 4.5, rond);
      final tp = TextPainter(
        text: TextSpan(text: y[i].toStringAsFixed(2), style: TextStyle(color: texte, fontSize: 11)),
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, pt(i) + Offset(-tp.width / 2, -22));
    }
  }

  @override
  bool shouldRepaint(covariant _Courbe old) => old.y != y || old.couleur != couleur || old.texte != texte;
}
