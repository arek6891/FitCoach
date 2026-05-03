package pl.fitcoach.features.clients.domain.repository

import pl.fitcoach.features.clients.domain.model.Client
import pl.fitcoach.features.clients.domain.model.TrainerProfile

interface ClientRepository {
    suspend fun getClients(): Result<List<Client>>
    suspend fun getTrainerProfile(): Result<TrainerProfile>
}
