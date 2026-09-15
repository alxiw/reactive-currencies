package io.github.alxiw.reactivecurrencies.domain.usecase

import io.github.alxiw.reactivecurrencies.domain.repository.CurrenciesRepository
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.math.BigDecimal

internal class ConvertValueUseCaseTest {

    private val repository: CurrenciesRepository = mock(CurrenciesRepository::class.java)
    private val useCase = ConvertValueUseCase(repository)

    @Test
    internal fun `invoke returns empty string when from is blank`() {
        val result = useCase("", "EUR", BigDecimal("10")).blockingGet()

        assertEquals("", result)
        verify(repository, never()).convertValue("", "EUR", BigDecimal("10"))
    }

    @Test
    internal fun `invoke returns empty string when to is blank`() {
        val result = useCase("USD", "", BigDecimal("10")).blockingGet()

        assertEquals("", result)
        verify(repository, never()).convertValue("USD", "", BigDecimal("10"))
    }

    @Test
    internal fun `invoke returns empty string when value is null`() {
        val result = useCase("USD", "EUR", null).blockingGet()

        assertEquals("", result)
        verify(repository, never()).convertValue("USD", "EUR", BigDecimal("10"))
    }

    @Test
    internal fun `invoke converts and formats value with two decimals`() {
        `when`(repository.convertValue("USD", "EUR", BigDecimal("10"))).thenReturn(Single.just(BigDecimal("9.5")))

        val result = useCase("USD", "EUR", BigDecimal("10")).blockingGet()

        assertEquals("9.50", result)
    }

    @Test
    internal fun `invoke rounds value to two decimals`() {
        `when`(repository.convertValue("USD", "EUR", BigDecimal("10"))).thenReturn(Single.just(BigDecimal("9.555")))

        val result = useCase("USD", "EUR", BigDecimal("10")).blockingGet()

        assertEquals("9.56", result)
    }

    @Test
    internal fun `invoke propagates repository error`() {
        val error = RuntimeException("convert failed")
        `when`(repository.convertValue("USD", "EUR", BigDecimal("10"))).thenReturn(Single.error(error))

        val result = useCase("USD", "EUR", BigDecimal("10")).test()

        result.assertError(error)
    }
}
