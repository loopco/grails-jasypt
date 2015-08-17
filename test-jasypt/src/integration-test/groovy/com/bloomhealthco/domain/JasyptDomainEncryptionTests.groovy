package com.bloomhealthco.domain

import grails.test.mixin.integration.Integration
import grails.transaction.Rollback
import groovy.sql.Sql
import org.junit.Test
import test.jasypt.domain.Patient
import static org.junit.Assert.*

@Integration
@Rollback
class JasyptDomainEncryptionTests  {
    def dataSource

    String CORRELATION_ID = "ABC123"


    @Test
    void testStringStringEncryption() {
        testPropertyAsStringEncryption('firstName', 'FIRST_NAME', 'foo')
    }

    @Test
    void testDateStringEncryption() {
        testPropertyAsStringEncryption('birthDate', 'BIRTH_DATE', new Date(1970, 2, 3))
    }

    @Test
    void testDoubleStringEncryption() {
        testPropertyAsStringEncryption('latitude', 'LATITUDE', 85.0d)
    }

    @Test
    void testBooleanStringEncryption() {
        testPropertyAsStringEncryption('hasInsurance', 'HAS_INSURANCE', true)
    }

    @Test
    void testFloatStringEncryption() {
        testPropertyAsStringEncryption('cashBalance', 'CASH_BALANCE', 123.45f)
    }

    @Test
    void testShortStringEncryption() {
        testPropertyAsStringEncryption('weight', 'WEIGHT', 160)
    }

    @Test
    void testIntegerStringEncryption() {
        testPropertyAsStringEncryption('height', 'HEIGHT', 74)
    }

    @Test
    void testLongStringEncryption() {
        testPropertyAsStringEncryption('patientId', 'PATIENT_ID', 1234567890)
    }

    @Test
    void testByteStringEncryption() {
        testPropertyAsStringEncryption('biteMe', 'BITE_ME', 2)
    }

    @Test
    void testSaltingEncryptsSameValueDifferentlyEachTime() {
        def originalPatient = new Patient(firstName: "foo", lastName: "foo", correlationId: CORRELATION_ID)
        originalPatient.save(failOnError: "true")

        withPatientForCorrelationId(CORRELATION_ID) { patient, rawPatient ->
            assertEquals "foo", patient.firstName
            assertEquals "foo", patient.lastName
            assertTrue "foo" != rawPatient.FIRST_NAME
            assertTrue "foo" != rawPatient.LAST_NAME
            assertTrue rawPatient.FIRST_NAME != rawPatient.LAST_NAME
        }
    }

    @Test
    void testEncryptionWithLongNamesFit() {
        def LONG_NAME_256 = "ABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOPABCDEFGHIJKLMNOP"

        (1..256).each { val ->
            def firstName = LONG_NAME_256.substring(0, val)
            def p = new Patient(firstName: firstName, correlationId: val).save()
            assertNotNull p

            withPatientForCorrelationId(val) { patient, rawPatient ->
                assertNotNull patient
                assertEquals firstName, patient.firstName
                // Bouncy Castle AES block encryption encrypts 256 character string in 384 characters
                assertTrue rawPatient.FIRST_NAME.size() <= 384
            }
        }
    }

    void testPropertyAsStringEncryption(property, rawProperty, value) {
        def originalPatient = new Patient(correlationId: CORRELATION_ID)
        originalPatient."$property" = value
        originalPatient.save(failOnError: "true")

        withPatientForCorrelationId(CORRELATION_ID) { patient, rawPatient ->
            assertTrue value == patient."$property"
            def rawPropertyValue = rawPatient."$rawProperty"
            assertTrue value.toString() != rawPropertyValue
            assertTrue rawPropertyValue.endsWith("=")
        }
    }

    def withPatientForCorrelationId(correlationId, closure) {
        def patient = Patient.findByCorrelationId(correlationId)
        assertNotNull patient
        retrieveRawPatientFromDatabase(correlationId) { rawPatient ->
            assertNotNull rawPatient
            closure(patient, rawPatient)
        }
    }

    def retrieveRawPatientFromDatabase(correlationId, closure) {
        new Sql(dataSource).with { db ->
            try {
                def result = db.firstRow("SELECT * FROM patient where correlation_id = $correlationId")
                closure(result)
            } finally {
                db.close()
            }
        }
    }
}
