package de.bitcoin.android.domain.connect_app

import de.bitcoin.android.data.source.account.AccountRepository
import de.bitcoin.android.domain.BaseUseCase
import de.bitcoin.android.domain.model.Either
import de.bitcoin.android.domain.model.Failure

class GetUsernameUseCase(private val repository: AccountRepository) : BaseUseCase<String, GetUsernameUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, String> {
        return try {
            val username = repository.getAccountConnect().username
            Either.Right(username)
        } catch (e: Exception) {
            Either.Left(GetUsernameFailure(e))
        }
    }

    class Params

    data class GetUsernameFailure(val error: Exception) : Failure.FeatureFailure(error)
}