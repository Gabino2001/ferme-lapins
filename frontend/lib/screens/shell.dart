import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../offline.dart';
import '../push.dart';
import '../ui.dart';
import 'argent.dart';
import 'assistant.dart';
import 'cages.dart';
import 'elevage.dart';
import 'equipe.dart';
import 'home.dart';
import 'lapins.dart';
import 'sante.dart';
import 'scanner.dart';
import 'stats.dart';

class Shell extends ConsumerStatefulWidget {
  const Shell({super.key});
  @override
  ConsumerState<Shell> createState() => _ShellState();
}

class _ShellState extends ConsumerState<Shell> {
  int i = 0;
  static const sections = {'lapins': 1, 'cages': 2, 'elevage': 3, 'sante': 4, 'argent': 5, 'clients': 5};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => initPush(ref));
  }

  Future<void> _menu(String v) async {
    final dio = ref.read(dioProvider);
    if (v == 'stats') {
      Navigator.push(context, MaterialPageRoute(builder: (_) => const StatsScreen()));
    } else if (v == 'equipe') {
      Navigator.push(context, MaterialPageRoute(builder: (_) => const EquipeScreen()));
    } else if (v == 'devise') {
      final r = await showForm(context, 'Devise des montants', [F('devise', 'Symbole ou code (ex. FCFA, EUR, \$)', init: deviseActuelle)]);
      if (r != null && r['devise'] != null && mounted) {
        await run(context, ref, () => dio.put('/api/config', data: {'devise': r['devise']}));
        ref.invalidate(configP);
      }
    } else if (v == 'theme') {
      final n = ref.read(themeModeProvider.notifier);
      final suivant = n.state == ThemeMode.light ? ThemeMode.dark : n.state == ThemeMode.dark ? ThemeMode.system : ThemeMode.light;
      n.state = suivant;
      await ref.read(prefsProvider).setString('theme', suivant.name);
    } else if (v == 'mdp') {
      final r = await showForm(context, 'Changer le mot de passe', [
        const F('ancien', 'Ancien mot de passe', obscure: true),
        const F('nouveau', 'Nouveau mot de passe (6 caractères minimum)', obscure: true),
      ]);
      if (r != null && mounted) await run(context, ref, () => dio.post('/api/compte/mot-de-passe', data: r));
    } else if (v == 'logout') {
      ref.read(authProvider.notifier).logout();
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.watch(realtimeProvider); // connexion temps réel active tant que l’utilisateur est connecté
    final role = ref.watch(authProvider.select((a) => a.role));
    final owner = role == null || role == 'PROPRIETAIRE';
    final theme = ref.watch(themeModeProvider);
    final cfg = ref.watch(configP).valueOrNull;
    if (cfg != null) deviseActuelle = '${cfg['devise'] ?? 'FCFA'}';

    final onglets = <(String, IconData, IconData, Widget)>[
      ('Accueil', Icons.home_outlined, Icons.home, HomeScreen(onGo: (s) => setState(() => i = sections[s] ?? 0))),
      ('Lapins', Icons.pets_outlined, Icons.pets, const LapinsScreen()),
      ('Cages', Icons.grid_view_outlined, Icons.grid_view, const CagesScreen()),
      ('Élevage', Icons.child_care_outlined, Icons.child_care, const ElevageScreen()),
      ('Santé', Icons.medical_services_outlined, Icons.medical_services, const SanteScreen()),
      if (owner) ('Argent', Icons.payments_outlined, Icons.payments, const ArgentScreen()),
    ];
    if (i >= onglets.length) i = 0;
    final large = MediaQuery.of(context).size.width >= 900;

    final contenu = Column(children: [
      Consumer(builder: (context, ref, _) {
        final offline = ref.watch(offlineStateProvider);
        final pending = ref.watch(pendingProvider);
        if (!offline && pending == 0) return const SizedBox.shrink();
        return Material(
          color: const Color(0x40B97B0B),
          child: ListTile(
            dense: true,
            leading: Icon(offline ? Icons.cloud_off : Icons.sync),
            title: Text(offline ? 'Hors-ligne : vos saisies seront envoyées au retour du réseau' : 'Synchronisation en cours'),
            subtitle: pending > 0 ? Text('$pending modification(s) en attente') : null,
            trailing: TextButton(onPressed: () => ref.read(offlineProvider).flush(), child: const Text('Réessayer')),
          ),
        );
      }),
      Expanded(child: onglets[i].$4),
    ]);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        title: const Text('Ferme des Lapins', style: TextStyle(fontWeight: FontWeight.w800)),
        actions: [
          const Padding(padding: EdgeInsets.only(right: 4), child: Icon(Icons.circle, size: 10, color: Color(0xFF2F6B4F))),
          const Center(child: Text('En direct  ', style: TextStyle(fontSize: 12))),
          IconButton(
            tooltip: 'Scanner un code QR',
            icon: const Icon(Icons.qr_code_scanner),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ScannerScreen())),
          ),
          IconButton(
            tooltip: 'Assistant',
            icon: const Icon(Icons.auto_awesome),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const AssistantScreen())),
          ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.account_circle_outlined),
            onSelected: _menu,
            itemBuilder: (_) => [
              if (owner) const PopupMenuItem(value: 'stats', child: Text('Indicateurs de performance')),
              if (owner) const PopupMenuItem(value: 'equipe', child: Text('Équipe et droits')),
              if (owner) PopupMenuItem(value: 'devise', child: Text('Devise : $deviseActuelle')),
              PopupMenuItem(value: 'theme', child: Text('Thème : ${theme == ThemeMode.light ? 'clair' : theme == ThemeMode.dark ? 'sombre' : 'automatique'}')),
              const PopupMenuItem(value: 'mdp', child: Text('Changer le mot de passe')),
              const PopupMenuItem(value: 'logout', child: Text('Déconnexion')),
            ],
          ),
        ],
      ),
      body: large
          ? Row(children: [
              NavigationRail(
                selectedIndex: i,
                onDestinationSelected: (v) => setState(() => i = v),
                labelType: NavigationRailLabelType.all,
                destinations: [for (final o in onglets) NavigationRailDestination(icon: Icon(o.$2), selectedIcon: Icon(o.$3), label: Text(o.$1))],
              ),
              const VerticalDivider(width: 1),
              Expanded(child: Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 900), child: contenu))),
            ])
          : contenu,
      bottomNavigationBar: large
          ? null
          : NavigationBar(
              selectedIndex: i,
              onDestinationSelected: (v) => setState(() => i = v),
              destinations: [for (final o in onglets) NavigationDestination(icon: Icon(o.$2), selectedIcon: Icon(o.$3), label: o.$1)],
            ),
    );
  }
}
