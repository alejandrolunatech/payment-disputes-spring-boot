package com.alejandrolunatech.disputes.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PiiMaskerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            mail me at jan.de.vries+bank@example.nl please | mail me at [EMAIL] please
            refund to NL91 ABNA 0417 1643 00             | refund to [IBAN]
            refund to NL91ABNA0417164300                 | refund to [IBAN]
            card 4111 1111 1111 1111 was used             | card [CARD] was used
            card 4111-1111-1111-1111 was used             | card [CARD] was used
            call +31 6 12345678 after five               | call [PHONE] after five
            call 0612345678 after five                   | call [PHONE] after five
            call +31 (0)6 1234 5678 after five           | call [PHONE] after five
            call +31(0)612345678 after five              | call [PHONE] after five
            order 12345 for 49.99 EUR on 12-10-2026       | order 12345 for 49.99 EUR on 12-10-2026
            """)
    void masksPersonalDataAndLeavesTheRest(String input, String expected) {
        assertThat(PiiMasker.mask(input)).isEqualTo(expected);
    }
}
