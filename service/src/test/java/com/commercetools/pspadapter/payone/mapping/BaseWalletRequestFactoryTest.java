package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.BaseTenantPropertyTest;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.common.RequestType;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletAuthorizationRequest;
import com.commercetools.pspadapter.payone.domain.payone.model.wallet.WalletPreauthorizationRequest;
import io.sphere.sdk.customers.Customer;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.payments.Payment;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import util.PaymentTestHelper;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static com.commercetools.pspadapter.payone.mapping.CustomFieldKeys.LANGUAGE_CODE_FIELD;
import static org.javamoney.moneta.function.MonetaryQueries.convertMinorPart;

public class BaseWalletRequestFactoryTest extends BaseTenantPropertyTest {

    protected final PaymentTestHelper payments = new PaymentTestHelper();
    protected WalletRequestFactory factory;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        factory = new WalletRequestFactory(tenantConfig);
    }

    protected final void createFullPreauthorizationRequestFromValidPayment(Payment payment,
                                                                           String expectedClearingtype,
                                                                           String expectedWalletType) throws Exception {
        Order order = payments.dummyOrderMapToPayoneRequest();
        Customer customer = payment.getCustomer().getObj();

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        WalletPreauthorizationRequest result = factory.createPreauthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.PREAUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(payoneConfig.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(payoneConfig.getKeyAsHash());
        softly.assertThat(result.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(result.getEncoding()).isEqualTo(payoneConfig.getEncoding());
        softly.assertThat(result.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        //clearing type
        softly.assertThat(result.getClearingtype()).isEqualTo(expectedClearingtype);
        softly.assertThat(result.getWallettype()).isEqualTo(expectedWalletType);
        softly.assertThat(result.getNoShipping()).isEqualTo(0);

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(customer.getCustomerNumber());
        // language set in payment object
        softly.assertThat(result.getLanguage()).isEqualTo(payment.getCustom().getFieldAsString(LANGUAGE_CODE_FIELD));

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(convertMinorPart().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        //urls
        softly.assertThat(result.getSuccessurl()).isEqualTo("www.test.de/success");
        softly.assertThat(result.getErrorurl()).isEqualTo("www.test.de/error");
        softly.assertThat(result.getBackurl()).isEqualTo("www.test.de/cancel");

        //billing address data
        Address billingAddress = order.getBillingAddress();
        softly.assertThat(result.getTitle()).isEqualTo(billingAddress.getTitle());
        softly.assertThat(result.getSalutation()).isEqualTo(billingAddress.getSalutation());
        softly.assertThat(result.getFirstname()).isEqualTo(billingAddress.getFirstName());
        softly.assertThat(result.getLastname()).isEqualTo(billingAddress.getLastName());
        softly.assertThat(result.getBirthday()).isEqualTo(payment.getCustomer().getObj().getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        softly.assertThat(result.getCompany()).isEqualTo(billingAddress.getCompany());
        softly.assertThat(result.getStreet()).isEqualTo(billingAddress.getStreetName() + " " + billingAddress.getStreetNumber());
        softly.assertThat(result.getAddressaddition()).isEqualTo(billingAddress.getAdditionalStreetInfo());
        softly.assertThat(result.getCity()).isEqualTo(billingAddress.getCity());
        softly.assertThat(result.getZip()).isEqualTo(order.getBillingAddress().getPostalCode());
        softly.assertThat(result.getCountry()).isEqualTo(order.getBillingAddress().getCountry().toLocale().getCountry());
        softly.assertThat(result.getEmail()).isEqualTo(order.getBillingAddress().getEmail());
        softly.assertThat(result.getTelephonenumber()).isEqualTo(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        //shipping address data
        Address shippingAddress = order.getShippingAddress();
        softly.assertThat(result.getShipping_firstname()).isEqualTo(shippingAddress.getFirstName());
        softly.assertThat(result.getShipping_lastname()).isEqualTo(shippingAddress.getLastName());
        softly.assertThat(result.getShipping_street()).isEqualTo(shippingAddress.getStreetName() + " " + shippingAddress.getStreetNumber());
        softly.assertThat(result.getShipping_city()).isEqualTo(shippingAddress.getCity());
        softly.assertThat(result.getShipping_zip()).isEqualTo(shippingAddress.getPostalCode());
        softly.assertThat(result.getShipping_country()).isEqualTo(shippingAddress.getCountry().toLocale().getCountry());
        softly.assertThat(result.getShipping_state()).isEqualTo(shippingAddress.getState());
        softly.assertThat(result.getShipping_company()).isEqualTo(shippingAddress.getCompany() + " " + shippingAddress.getDepartment());

        softly.assertAll();
    }

    protected void createFullAuthorizationRequestFromValidPayment(Payment payment,
                                                                  String expectedClearingtype,
                                                                  String expectedWalletType) throws Exception {

        Order order = payments.dummyOrderMapToPayoneRequest();
        Customer customer = payment.getCustomer().getObj();

        PaymentWithCartLike paymentWithCartLike = new PaymentWithCartLike(payment, order);
        WalletAuthorizationRequest result = factory.createAuthorizationRequest(paymentWithCartLike);
        SoftAssertions softly = new SoftAssertions();

        //base values
        softly.assertThat(result.getRequest()).isEqualTo(RequestType.AUTHORIZATION.getType());
        softly.assertThat(result.getAid()).isEqualTo(payoneConfig.getSubAccountId());
        softly.assertThat(result.getMid()).isEqualTo(payoneConfig.getMerchantId());
        softly.assertThat(result.getPortalid()).isEqualTo(payoneConfig.getPortalId());
        softly.assertThat(result.getKey()).isEqualTo(payoneConfig.getKeyAsHash());
        softly.assertThat(result.getMode()).isEqualTo(payoneConfig.getMode());
        softly.assertThat(result.getApiVersion()).isEqualTo(payoneConfig.getApiVersion());
        softly.assertThat(result.getEncoding()).isEqualTo(payoneConfig.getEncoding());
        softly.assertThat(result.getSolutionName()).isEqualTo(payoneConfig.getSolutionName());
        softly.assertThat(result.getSolutionVersion()).isEqualTo(payoneConfig.getSolutionVersion());
        softly.assertThat(result.getIntegratorName()).isEqualTo(payoneConfig.getIntegratorName());
        softly.assertThat(result.getIntegratorVersion()).isEqualTo(payoneConfig.getIntegratorVersion());

        //clearing type
        softly.assertThat(result.getClearingtype()).isEqualTo(expectedClearingtype);
        softly.assertThat(result.getWallettype()).isEqualTo(expectedWalletType);
        softly.assertThat(result.getNoShipping()).isEqualTo(0);

        //references
        softly.assertThat(result.getReference()).isEqualTo(paymentWithCartLike.getReference());
        softly.assertThat(result.getCustomerid()).isEqualTo(customer.getCustomerNumber());
        // language set in payment object
        softly.assertThat(result.getLanguage()).isEqualTo(payment.getCustom().getFieldAsString(LANGUAGE_CODE_FIELD));

        //monetary
        softly.assertThat(result.getAmount()).isEqualTo(convertMinorPart().queryFrom(payment.getAmountPlanned()).intValue());
        softly.assertThat(result.getCurrency()).isEqualTo(payment.getAmountPlanned().getCurrency().getCurrencyCode());

        //urls
        softly.assertThat(result.getSuccessurl()).isEqualTo("www.test.de/success");
        softly.assertThat(result.getErrorurl()).isEqualTo("www.test.de/error");
        softly.assertThat(result.getBackurl()).isEqualTo("www.test.de/cancel");

        //billing address data
        Address billingAddress = order.getBillingAddress();
        softly.assertThat(result.getTitle()).isEqualTo(billingAddress.getTitle());
        softly.assertThat(result.getSalutation()).isEqualTo(billingAddress.getSalutation());
        softly.assertThat(result.getFirstname()).isEqualTo(billingAddress.getFirstName());
        softly.assertThat(result.getLastname()).isEqualTo(billingAddress.getLastName());
        softly.assertThat(result.getBirthday()).isEqualTo(payment.getCustomer().getObj().getDateOfBirth().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        softly.assertThat(result.getCompany()).isEqualTo(billingAddress.getCompany());
        softly.assertThat(result.getStreet()).isEqualTo(billingAddress.getStreetName() + " " + billingAddress.getStreetNumber());
        softly.assertThat(result.getAddressaddition()).isEqualTo(billingAddress.getAdditionalStreetInfo());
        softly.assertThat(result.getCity()).isEqualTo(billingAddress.getCity());
        softly.assertThat(result.getZip()).isEqualTo(order.getBillingAddress().getPostalCode());
        softly.assertThat(result.getCountry()).isEqualTo(order.getBillingAddress().getCountry().toLocale().getCountry());
        softly.assertThat(result.getEmail()).isEqualTo(order.getBillingAddress().getEmail());
        softly.assertThat(result.getTelephonenumber()).isEqualTo(Optional
                .ofNullable(billingAddress.getPhone())
                .orElse(billingAddress.getMobile()));

        //shipping address data
        Address shippingAddress = order.getShippingAddress();
        softly.assertThat(result.getShipping_firstname()).isEqualTo(shippingAddress.getFirstName());
        softly.assertThat(result.getShipping_lastname()).isEqualTo(shippingAddress.getLastName());
        softly.assertThat(result.getShipping_street()).isEqualTo(shippingAddress.getStreetName() + " " + shippingAddress.getStreetNumber());
        softly.assertThat(result.getShipping_city()).isEqualTo(shippingAddress.getCity());
        softly.assertThat(result.getShipping_zip()).isEqualTo(shippingAddress.getPostalCode());
        softly.assertThat(result.getShipping_country()).isEqualTo(shippingAddress.getCountry().toLocale().getCountry());
        softly.assertThat(result.getShipping_state()).isEqualTo(shippingAddress.getState());
        softly.assertThat(result.getShipping_company()).isEqualTo(shippingAddress.getCompany() + " " + shippingAddress.getDepartment());

        softly.assertAll();
    }
}
