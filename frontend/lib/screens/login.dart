import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core.dart';
import '../ui.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});
  @override
  ConsumerState<LoginScreen> createState() => _LoginState();
}

class _LoginState extends ConsumerState<LoginScreen> {
  final nom = TextEditingController(), email = TextEditingController(), pwd = TextEditingController();
  bool inscription = false, busy = false;
  String? erreur;

  Future<void> submit() async {
    setState(() { busy = true; erreur = null; });
    try {
      final a = ref.read(authProvider.notifier);
      if (inscription) {
        await a.register(nom.text, email.text, pwd.text);
      } else {
        await a.login(email.text, pwd.text);
      }
    } catch (e) {
      if (mounted) setState(() => erreur = errMsg(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> oublie() async {
    final r1 = await showForm(context, 'Mot de passe oublié', [F('email', 'Votre email', init: email.text)]);
    if (r1 == null || r1['email'] == null) return;
    final adresse = '${r1['email']}';
    try {
      await ref.read(authProvider.notifier).demanderCode(adresse);
    } catch (e) {
      if (mounted) setState(() => erreur = errMsg(e));
      return;
    }
    if (!mounted) return;
    final r2 = await showForm(context, 'Code reçu par email', [
      const F('code', 'Code à 6 chiffres'),
      const F('nouveau', 'Nouveau mot de passe (6 caractères minimum)', obscure: true),
    ]);
    if (r2 == null) return;
    try {
      await ref.read(authProvider.notifier).reinitialiser(adresse, '${r2['code']}', '${r2['nouveau']}');
      if (mounted) {
        setState(() {
          email.text = adresse;
          erreur = null;
        });
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Mot de passe modifié : connectez-vous')));
      }
    } catch (e) {
      if (mounted) setState(() => erreur = errMsg(e));
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 420),
                child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                  const Text('🐇', textAlign: TextAlign.center, style: TextStyle(fontSize: 56)),
                  const Text('Ferme des Lapins', textAlign: TextAlign.center, style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800)),
                  const SizedBox(height: 24),
                  if (inscription) TextField(controller: nom, decoration: const InputDecoration(labelText: 'Votre nom')),
                  TextField(controller: email, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Email')),
                  TextField(controller: pwd, obscureText: true, decoration: const InputDecoration(labelText: 'Mot de passe (6 caractères minimum)')),
                  if (erreur != null) Padding(padding: const EdgeInsets.only(top: 12), child: Text(erreur!, style: const TextStyle(color: Colors.red))),
                  const SizedBox(height: 20),
                  FilledButton(
                    onPressed: busy ? null : submit,
                    child: Padding(padding: const EdgeInsets.all(6), child: Text(busy ? '...' : inscription ? 'Créer mon compte' : 'Se connecter')),
                  ),
                  TextButton(
                    onPressed: () => setState(() => inscription = !inscription),
                    child: Text(inscription ? 'J’ai déjà un compte' : 'Créer un compte'),
                  ),
                  TextButton(onPressed: oublie, child: const Text('Mot de passe oublié ?')),
                ]),
              ),
            ),
          ),
        ),
      );
}
