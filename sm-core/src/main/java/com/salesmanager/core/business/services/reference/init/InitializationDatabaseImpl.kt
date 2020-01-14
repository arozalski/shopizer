package com.salesmanager.core.business.services.reference.init

import com.salesmanager.core.business.exception.ServiceException
import com.salesmanager.core.business.services.catalog.category.CategoryService
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService
import com.salesmanager.core.business.services.catalog.product.type.ProductTypeService
import com.salesmanager.core.business.services.customer.CustomerService
import com.salesmanager.core.business.services.merchant.MerchantStoreService
import com.salesmanager.core.business.services.reference.country.CountryService
import com.salesmanager.core.business.services.reference.currency.CurrencyService
import com.salesmanager.core.business.services.reference.language.LanguageService
import com.salesmanager.core.business.services.reference.loader.IntegrationModulesLoader
import com.salesmanager.core.business.services.reference.loader.ZonesLoader
import com.salesmanager.core.business.services.reference.zone.ZoneService
import com.salesmanager.core.business.services.system.ModuleConfigurationService
import com.salesmanager.core.business.services.system.optin.OptinService
import com.salesmanager.core.business.services.tax.TaxClassService
import com.salesmanager.core.constants.SchemaConstant
import com.salesmanager.core.model.catalog.category.Category
import com.salesmanager.core.model.catalog.category.CategoryDescription
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer
import com.salesmanager.core.model.catalog.product.manufacturer.ManufacturerDescription
import com.salesmanager.core.model.catalog.product.type.ProductType
import com.salesmanager.core.model.common.Billing
import com.salesmanager.core.model.common.Delivery
import com.salesmanager.core.model.customer.Customer
import com.salesmanager.core.model.customer.CustomerGender
import com.salesmanager.core.model.merchant.MerchantStore
import com.salesmanager.core.model.reference.country.Country
import com.salesmanager.core.model.reference.country.CountryDescription
import com.salesmanager.core.model.reference.currency.Currency
import com.salesmanager.core.model.reference.language.Language
import com.salesmanager.core.model.system.optin.Optin
import com.salesmanager.core.model.system.optin.OptinType
import com.salesmanager.core.model.tax.taxclass.TaxClass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.util.*
import javax.inject.Inject

@Service("initializationDatabase")
open class InitializationDatabaseImpl : InitializationDatabase {

    @Inject
    lateinit var zoneService: ZoneService
    @Inject
    lateinit var languageService: LanguageService
    @Inject
    lateinit var countryService: CountryService
    @Inject
    lateinit var currencyService: CurrencyService
    @Inject
    lateinit var merchantService: MerchantStoreService
    @Inject
    lateinit var productTypeService: ProductTypeService
    @Inject
    lateinit var taxClassService: TaxClassService
    @Inject
    lateinit var zonesLoader: ZonesLoader
    @Inject
    lateinit var modulesLoader: IntegrationModulesLoader
    @Inject
    lateinit var manufacturerService: ManufacturerService
    @Inject
    lateinit var moduleConfigurationService: ModuleConfigurationService
    @Inject
    lateinit var optinService: OptinService
    @Inject
    lateinit var categoryService: CategoryService
    @Inject
    lateinit var customerService: CustomerService
    private var name: String? = null

    override fun isEmpty() = languageService.count() == 0L

    @Transactional
    @Throws(ServiceException::class)
    override fun populate(contextName: String) {
        this.name = contextName
        createLanguages()
        createCountries()
        createZones()
        createCurrencies()
        createSubReferences()
        createModules()
        createMerchant()
        val store = merchantService.getMerchantStore(MerchantStore.DEFAULT_STORE)
        val language = languageService.defaultLanguage()
        createCategory(store, language)
        val country = countryService.getByCode("PL")
        createCustomer(store, language, country)
    }

    @Throws(ServiceException::class)
    private fun createCurrencies() {
        LOGGER.info(String.format("%s : Populating Currencies ", name))

        for (code in SchemaConstant.CURRENCY_MAP.keys) {
            try {
                val c = java.util.Currency.getInstance(code)

                if (c == null) {
                    LOGGER.info(String.format("%s : Populating Currencies : no currency for code : %s", name, code))
                }
                //check if it exist
                val currency = Currency()
                currency.name = c!!.currencyCode
                currency.currency = c
                currencyService.create(currency)
                //System.out.println(l.getCountry() + "   " + c.getSymbol() + "  " + c.getSymbol(l));
            } catch (e: IllegalArgumentException) {
                LOGGER.info(String.format("%s : Populating Currencies : no currency for code : %s", name, code))
            }
        }
    }

    @Throws(ServiceException::class)
    private fun createCountries() {
        LOGGER.info(String.format("%s : Populating Countries ", name))
        val languages = languageService.list()
        for (code in SchemaConstant.COUNTRY_ISO_CODE) {
            val locale = SchemaConstant.LOCALES[code]
            if (locale != null) {
                val country = Country(code)
                countryService.create(country)

                for (language in languages) {
                    val name = locale.getDisplayCountry(Locale(language.code))
                    //byte[] ptext = value.getBytes(Constants.ISO_8859_1);
                    //String name = new String(ptext, Constants.UTF_8);
                    val description = CountryDescription(language, name)
                    countryService.addCountryDescription(country, description)
                }
            }
        }
    }

    @Throws(ServiceException::class)
    private fun createZones() {
        LOGGER.info(String.format("%s : Populating Zones ", name))
        try {
            val zonesMap = zonesLoader.loadZones("reference/zoneconfig.json")

            for ((key, value) in zonesMap) {
                if (value.descriptions == null) {
                    LOGGER.warn("This zone $key has no descriptions")
                    continue
                }
                val zoneDescriptions = value.descriptions
                value.setDescriptons(null)

                zoneService.create(value)

                for (description in zoneDescriptions) {
                    description.zone = value
                    zoneService.addDescription(value, description)
                }
            }
        } catch (e: Exception) {
            throw ServiceException(e)
        }
    }

    @Throws(ServiceException::class)
    private fun createLanguages() {
        LOGGER.info(String.format("%s : Populating Languages ", name))
        for (code in SchemaConstant.LANGUAGE_ISO_CODE) {
            val language = Language(code)
            languageService.create(language)
        }
    }

    @Throws(ServiceException::class)
    private fun createMerchant() {
        LOGGER.info(String.format("%s : Creating merchant ", name))
        val date = Date(System.currentTimeMillis())
        val en = languageService.getByCode("en")
        val ca = countryService.getByCode("CA")
        val currency = currencyService.getByCode("CAD")
        val qc = zoneService.getByCode("QC")
        val supportedLanguages = ArrayList<Language>()
        supportedLanguages.add(en)
        //create a merchant
        val store = MerchantStore()
        store.country = ca
        store.currency = currency
        store.defaultLanguage = en
        store.inBusinessSince = date
        store.zone = qc
        store.storename = "Default store"
        store.storephone = "888-888-8888"
        store.code = MerchantStore.DEFAULT_STORE
        store.storecity = "My city"
        store.storeaddress = "1234 Street address"
        store.storepostalcode = "H2H-2H2"
        store.storeEmailAddress = "john@test.com"
        store.domainName = "localhost:8080"
        store.storeTemplate = "generic"
        store.languages = supportedLanguages

        merchantService.create(store)
        val taxclass = TaxClass(TaxClass.DEFAULT_TAX_CLASS)
        taxclass.merchantStore = store

        taxClassService.create(taxclass)
        //create default manufacturer
        val defaultManufacturer = Manufacturer()
        defaultManufacturer.code = Manufacturer.DEFAULT_MANUFACTURER
        defaultManufacturer.merchantStore = store
        val manufacturerDescription = ManufacturerDescription()
        manufacturerDescription.language = en
        manufacturerDescription.name = "DEFAULT"
        manufacturerDescription.manufacturer = defaultManufacturer
        manufacturerDescription.description = "DEFAULT"
        defaultManufacturer.descriptions.add(manufacturerDescription)

        manufacturerService.create(defaultManufacturer)
        val newsletter = Optin()
        newsletter.code = OptinType.NEWSLETTER.name
        newsletter.merchant = store
        newsletter.optinType = OptinType.NEWSLETTER
        optinService.create(newsletter)
    }

    @Throws(ServiceException::class)
    private fun createCategory(store: MerchantStore, language: Language) {
        val rootCategory = Category()
        rootCategory.merchantStore = store
        rootCategory.code = Category.DEFAULT_CATEGORY
        rootCategory.isVisible = true
        val description = CategoryDescription()
        description.name = "All"
        description.category = rootCategory
        description.language = language
        description.seUrl = "all"
        rootCategory.descriptions.add(description)
        categoryService.create(rootCategory)
    }

    @Throws(ServiceException::class)
    private fun createCustomer(store: MerchantStore, language: Language, country: Country) {
        Customer().apply {
            merchantStore = store
            emailAddress = "robert.lewandowski@shopizer.com"
            gender = CustomerGender.M
            isAnonymous = false
            defaultLanguage = language
            nick = "RL9"
            password = "password"
            delivery = Delivery()
            billing = Billing().apply {
                firstName = "Robert"
                lastName = "Lewandowski"
                this.country = country
            }
        }.let(customerService::saveOrUpdate)
    }

    @Throws(ServiceException::class)
    private fun createModules() {
        try {
            val modules = modulesLoader.loadIntegrationModules("reference/integrationmodules.json")
            for (entry in modules) {
                moduleConfigurationService.create(entry)
            }
        } catch (e: Exception) {
            throw ServiceException(e)
        }
    }

    @Throws(ServiceException::class)
    private fun createSubReferences() {
        LOGGER.info(String.format("%s : Loading catalog sub references ", name))
        val productType = ProductType()
        productType.code = ProductType.GENERAL_TYPE
        productTypeService.create(productType)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(InitializationDatabaseImpl::class.java)
    }
}
