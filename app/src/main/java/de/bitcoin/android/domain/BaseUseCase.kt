package de.bitcoin.android.domain

import com.google.gson.JsonSyntaxException
import de.bitcoin.android.data.model.remote.response.BaseResponse
import de.bitcoin.android.di.getGson
import de.bitcoin.android.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseUseCase<out Type, in Params> where Type : Any {

    abstract suspend fun run(params: Params): Either<Failure, Type>

    open operator fun invoke(scope: CoroutineScope, params: Params, onResult: (Either<Failure, Type>) -> Unit = {}) {
        val backgroundJob = scope.async { run(params) }
        scope.launch { onResult(backgroundJob.await()) }
    }

    protected fun onWrapException(exception: Exception): Failure {
        return when (exception) {
            is UnknownHostException, is SocketTimeoutException, is SocketException -> NetworkFailure()
            is HttpException -> {
                try {
                    val response = getGson().fromJson(exception.response()?.errorBody()?.string(), BaseResponse::class.java)
                    val errors = response.errors
                    if (errors?.isNotEmpty() == true) {
                        ApiFailure(errors)
                    } else {
                        UnknownFailure()
                    }
                } catch (e: JsonSyntaxException) {
                    UnknownFailure()
                }
            }
            else -> UnknownFailure()
        }
    }
}