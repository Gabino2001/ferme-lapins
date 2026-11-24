package com.ferme.lapins.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() {}
    public record Register(@NotBlank String nom, @NotBlank @Email String email, @Size(min = 6) String motDePasse) {}
    public record Login(String email, String motDePasse) {}
    public record AuthResponse(String token, String nom, String email, String role) {}
    public record NouveauMembre(@NotBlank String nom, @NotBlank @Email String email, @Size(min = 6) String motDePasse, @NotBlank String role) {}
    public record MiseBas(LocalDate date, int nes, int vivants) {}
    public record Sevrage(int nombre, String cage) {}
    public record VenteLot(int quantite, double prixUnitaire, LocalDate date) {}
    public record PerteLot(int nombre) {}
    public record PeseeReq(double poids, LocalDate date) {}
    public record ChangerMotDePasse(@NotBlank String ancien, @Size(min = 6) String nouveau) {}
    public record NouveauMdp(@Size(min = 6) String nouveau) {}
    public record Devise(@NotBlank String devise) {}
    public record DemandeCode(String email) {}
    public record Reinitialisation(String email, String code, String nouveau) {}
    public record MessageChat(String role, String content) {}
    public record QuestionAssistant(String question, List<MessageChat> historique, String photo) {}
}
