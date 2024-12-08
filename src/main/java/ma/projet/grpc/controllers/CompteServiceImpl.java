package ma.projet.grpc.controllers;

import io.grpc.stub.StreamObserver;
import ma.projet.grpc.stubs.*;
import ma.projet.entities.Compte;
import ma.projet.repositories.CompteRepository;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@GrpcService
public class CompteServiceImpl extends CompteServiceGrpc.CompteServiceImplBase {

    @Autowired
    private CompteRepository compteRepository;

    @Override
    public void deleteCompte(DeleteCompteRequest request, StreamObserver<DeleteCompteResponse> responseObserver) {
        try {
            Long id = Long.parseLong(request.getId());
            Optional<Compte> optionalCompte = compteRepository.findById(id);

            if (optionalCompte.isPresent()) {
                compteRepository.delete(optionalCompte.get());
                responseObserver.onNext(DeleteCompteResponse.newBuilder().setSuccess(true).build());
            } else {
                responseObserver.onError(new Throwable("Compte with ID " + id + " not found."));
            }
            responseObserver.onCompleted();
        } catch (NumberFormatException e) {
            responseObserver.onError(new Throwable("Invalid ID format. ID must be a number."));
        } catch (Exception e) {
            responseObserver.onError(new Throwable("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @Override
    public void findComptesByType(FindComptesByTypeRequest request, StreamObserver<FindComptesByTypeResponse> responseObserver) {
        try {
            ma.projet.entities.TypeCompte type = request.getType() == ma.projet.grpc.stubs.TypeCompte.COURANT ?
                    ma.projet.entities.TypeCompte.COURANT : ma.projet.entities.TypeCompte.EPARGNE;
            List<Compte> comptes = compteRepository.findByType(type);
            FindComptesByTypeResponse.Builder responseBuilder = FindComptesByTypeResponse.newBuilder();

            for (Compte compte : comptes) {
                responseBuilder.addComptes(mapToGrpcCompte(compte));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new Throwable("An unexpected error occurred: " + e.getMessage()));
        }
    }
    @Override
    public void allComptes(GetAllComptesRequest request, StreamObserver<GetAllComptesResponse> responseObserver) {
        try {
            List<Compte> comptes = compteRepository.findAll();
            GetAllComptesResponse.Builder responseBuilder = GetAllComptesResponse.newBuilder();

            for (Compte compte : comptes) {
                responseBuilder.addComptes(mapToGrpcCompte(compte));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new Throwable("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @Override
    public void compteById(GetCompteByIdRequest request, StreamObserver<GetCompteByIdResponse> responseObserver) {
        try {
            Long id = Long.parseLong(request.getId());
            Optional<Compte> optionalCompte = compteRepository.findById(id);

            if (optionalCompte.isPresent()) {
                responseObserver.onNext(GetCompteByIdResponse.newBuilder()
                        .setCompte(mapToGrpcCompte(optionalCompte.get()))
                        .build());
            } else {
                responseObserver.onError(new Throwable("Compte with ID " + id + " not found."));
            }
            responseObserver.onCompleted();
        } catch (NumberFormatException e) {
            responseObserver.onError(new Throwable("Invalid ID format. ID must be a number."));
        } catch (Exception e) {
            responseObserver.onError(new Throwable("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @Override
    public void totalSolde(GetTotalSoldeRequest request, StreamObserver<GetTotalSoldeResponse> responseObserver) {
        try {
            List<Compte> comptes = compteRepository.findAll();
            int count = comptes.size();
            double sum = comptes.stream().map(Compte::getSolde).reduce(0.0, Double::sum);
            double average = count > 0 ? sum / count : 0;

            SoldeStats stats = SoldeStats.newBuilder()
                    .setCount(count)
                    .setSum((float) sum)
                    .setAverage((float) average)
                    .build();

            responseObserver.onNext(GetTotalSoldeResponse.newBuilder().setStats(stats).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new Throwable("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @Override
    public void saveCompte(SaveCompteRequest request, StreamObserver<SaveCompteResponse> responseObserver) {
        CompteRequest compteReq = request.getCompte();

        String pattern = "MM-dd-yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

        Compte newCompte = new Compte();
        newCompte.setSolde(compteReq.getSolde());

        try {
            // Parse date
            Date date = simpleDateFormat.parse(compteReq.getDateCreation());
            newCompte.setDateCreation(date);
            newCompte.setType(compteReq.getType().getNumber() == 0 ?
                    ma.projet.entities.TypeCompte.COURANT : ma.projet.entities.TypeCompte.EPARGNE);

            // Save the entity
            Compte savedCompte = compteRepository.save(newCompte);

            // Map back to gRPC Compte
            responseObserver.onNext(SaveCompteResponse.newBuilder()
                    .setCompte(mapToGrpcCompte(savedCompte))
                    .build());
            responseObserver.onCompleted();
        } catch (ParseException e) {
            responseObserver.onError(new Throwable("Invalid date format. Expected format is MM-dd-yyyy."));
        } catch (Exception e) {
            responseObserver.onError(new Throwable("An unexpected error occurred: " + e.getMessage()));
        }
    }

    // Utility function to map JPA entity to gRPC Compte
    private ma.projet.grpc.stubs.Compte mapToGrpcCompte(Compte compte) {
        ma.projet.grpc.stubs.TypeCompte type = compte.getType() == ma.projet.entities.TypeCompte.COURANT ?
                ma.projet.grpc.stubs.TypeCompte.COURANT : ma.projet.grpc.stubs.TypeCompte.EPARGNE;
        return ma.projet.grpc.stubs.Compte.newBuilder()
                .setId(String.valueOf(compte.getId()))
                .setSolde((float) compte.getSolde())
                .setDateCreation(compte.getDateCreation().toString())
                .setType(type)
                .build();
    }
}