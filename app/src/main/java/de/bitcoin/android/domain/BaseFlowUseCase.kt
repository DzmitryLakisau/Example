package de.bitcoin.android.domain

import kotlinx.coroutines.flow.Flow

abstract class BaseFlowUseCase<out Type, in Params> {

    abstract fun execute(params: Params): Flow<Type?>
}