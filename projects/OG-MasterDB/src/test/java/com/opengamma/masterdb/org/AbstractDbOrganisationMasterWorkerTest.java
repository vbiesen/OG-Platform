/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.masterdb.org;

import com.opengamma.core.obligor.CreditRating;
import com.opengamma.core.obligor.CreditRatingFitch;
import com.opengamma.core.obligor.CreditRatingMoodys;
import com.opengamma.core.obligor.CreditRatingStandardAndPoors;
import com.opengamma.core.obligor.Region;
import com.opengamma.core.obligor.Sector;
import com.opengamma.id.ExternalId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.orgs.ManageableOrganisation;
import com.opengamma.master.orgs.OrganisationDocument;
import com.opengamma.masterdb.DbMasterTestUtils;
import com.opengamma.masterdb.orgs.DbOrganisationMaster;
import com.opengamma.util.test.DbTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.threeten.bp.Clock;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneOffset;

import static com.opengamma.util.db.DbDateUtils.MAX_SQL_TIMESTAMP;
import static com.opengamma.util.db.DbDateUtils.toSqlTimestamp;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Base tests for DbOrganisationMasterWorker via DbOrganisationMaster.
 */
public abstract class AbstractDbOrganisationMasterWorkerTest extends DbTest {

  private static final Logger s_logger = LoggerFactory.getLogger(AbstractDbOrganisationMasterWorkerTest.class);

  protected DbOrganisationMaster _orgMaster;
  protected Instant _version1Instant;
  protected Instant _version2Instant;
  protected int _totalOrganisations;
  protected boolean _readOnly;  // attempt to speed up tests

  final static String PROVIDER_SCHEME = "Markit";

  public AbstractDbOrganisationMasterWorkerTest(String databaseType, String databaseVersion, boolean readOnly) {
    super(databaseType, databaseVersion, databaseVersion);
    _readOnly = readOnly;
    s_logger.info("running testcases for {}", databaseType);
  }

  @BeforeClass
  public void setUpClass() throws Exception {
    if (_readOnly) {
      init();
    }
  }

  @BeforeMethod
  public void setUp() throws Exception {
    if (_readOnly == false) {
      init();
    }
  }

  private void init() throws Exception {
    super.setUp();
    ConfigurableApplicationContext context = DbMasterTestUtils.getContext(getDatabaseType());
    _orgMaster = (DbOrganisationMaster) context.getBean(getDatabaseType() + "DbOrganisationMaster");

//    id bigint NOT NULL,
//    oid bigint NOT NULL,
//    ver_from_instant timestamp without time zone NOT NULL,
//    ver_to_instant timestamp without time zone NOT NULL,
//    corr_from_instant timestamp without time zone NOT NULL,
//    corr_to_instant timestamp without time zone NOT NULL,
//    provider_scheme varchar(255),
//    provider_value varchar(255),
//
//    obligor_short_name                           varchar(255) NOT NULL,
//    obligor_red_code                             varchar(255) NOT NULL,
//    obligor_ticker                               varchar(255) NOT NULL,
//    obligor_country                              varchar(255) NOT NULL,
//    obligor_region                               varchar(255) NOT NULL,
//    obligor_sector                               varchar(255) NOT NULL,
//    obligor_composite_rating                     varchar(255) NOT NULL,
//    obligor_implied_rating                       varchar(255) NOT NULL,
//    obligor_fitch_credit_rating                  varchar(255) NOT NULL,
//    obligor_moodys_credit_rating                 varchar(255) NOT NULL,
//    obligor_standard_and_poors_credit_rating     varchar(255) NOT NULL,
//    obligor_has_defaulted                        smallint NOT NULL,

    Instant now = Instant.now();
    _orgMaster.setClock(Clock.fixed(now, ZoneOffset.UTC));
    _version1Instant = now.minusSeconds(100);
    _version2Instant = now.minusSeconds(50);
    s_logger.debug("test data now:   {}", _version1Instant);
    s_logger.debug("test data later: {}", _version2Instant);

    final SimpleJdbcTemplate template = _orgMaster.getDbConnector().getJdbcTemplate();

//    id bigint NOT NULL,
//    oid bigint NOT NULL,
//    ver_from_instant timestamp without time zone NOT NULL,
//        ver_to_instant timestamp without time zone NOT NULL,
//    corr_from_instant timestamp without time zone NOT NULL,
//        corr_to_instant timestamp without time zone NOT NULL,
//    provider_scheme varchar(255),
//        provider_value varchar(255),
//
//        obligor_short_name                           varchar(255) NOT NULL,
//    obligor_red_code                             varchar(255) NOT NULL,
//    obligor_ticker                               varchar(255) NOT NULL,
//    obligor_country                              varchar(255) NOT NULL,
//    obligor_region                               varchar(255) NOT NULL,
//    obligor_sector                               varchar(255) NOT NULL,
//    obligor_composite_rating                     varchar(255) NOT NULL,
//    obligor_implied_rating                       varchar(255) NOT NULL,
//    obligor_fitch_credit_rating                  varchar(255) NOT NULL,
//    obligor_moodys_credit_rating                 varchar(255) NOT NULL,
//    obligor_standard_and_poors_credit_rating     varchar(255) NOT NULL,
//    obligor_has_defaulted                        smallint NOT NULL,
//
    template.update("INSERT INTO org_organisation VALUES (?,?,?,?,?,?,?,?, ?,?,?,?,?,?,?,?,?,?,?,?)",
                    101,
                    101,
                    toSqlTimestamp(_version1Instant),
                    MAX_SQL_TIMESTAMP,
                    toSqlTimestamp(_version1Instant),
                    MAX_SQL_TIMESTAMP,
                    PROVIDER_SCHEME,
                    "1",
                    "TestOrganisation101",
                    "RED_code_101",
                    "ticker_101",
                    "CountryA",
        Region.AFRICA.name(),
        Sector.BASICMATERIALS.name(),
        CreditRating.A.name(),
        CreditRating.A.name(),
        CreditRatingFitch.A.name(),
        CreditRatingMoodys.A.name(),
        CreditRatingStandardAndPoors.A.name(),
        0);
    template.update("INSERT INTO org_organisation VALUES (?,?,?,?,?,?,?,?, ?,?,?,?,?,?,?,?,?,?,?,?)",
                    102,
                    102,
                    toSqlTimestamp(_version1Instant),
                    MAX_SQL_TIMESTAMP,
                    toSqlTimestamp(_version1Instant),
                    MAX_SQL_TIMESTAMP,
                    PROVIDER_SCHEME,
                    "2",
                    "TestOrganisation102",
                    "RED_code_102",
                    "ticker_102",
                    "CountryB",
        Region.AFRICA.name(),
        Sector.BASICMATERIALS.name(),
        CreditRating.A.name(),
        CreditRating.A.name(),
        CreditRatingFitch.A.name(),
        CreditRatingMoodys.A.name(),
        CreditRatingStandardAndPoors.A.name(),
        0);

    template.update("INSERT INTO org_organisation VALUES (?,?,?,?,?,?,?,?, ?,?,?,?,?,?,?,?,?,?,?,?)",
                    201,
                    201,
                    toSqlTimestamp(_version1Instant),
        toSqlTimestamp(_version2Instant),
        toSqlTimestamp(_version1Instant),
                    MAX_SQL_TIMESTAMP,
                    PROVIDER_SCHEME,
                    "3",
                    "TestOrganisation201",
                    "RED_code_201",
                    "ticker_201",
                    "CountryC",
        Region.AFRICA.name(),
        Sector.BASICMATERIALS.name(),
        CreditRating.A.name(),
        CreditRating.A.name(),
        CreditRatingFitch.A.name(),
        CreditRatingMoodys.A.name(),
        CreditRatingStandardAndPoors.A.name(),
        0);
    template.update("INSERT INTO org_organisation VALUES (?,?,?,?,?,?,?,?, ?,?,?,?,?,?,?,?,?,?,?,?)",
                    202,
                    201,
                    toSqlTimestamp(_version2Instant),
                    MAX_SQL_TIMESTAMP,
                    toSqlTimestamp(_version2Instant),
                    MAX_SQL_TIMESTAMP,
                    PROVIDER_SCHEME,
                    "3",
                    "TestOrganisation201",
                    "RED_code_201",
                    "ticker_201",
                    "CountryD",
        Region.AFRICA.name(),
        Sector.BASICMATERIALS.name(),
        CreditRating.B.name(),
        CreditRating.B.name(),
        CreditRatingFitch.B.name(),
        CreditRatingMoodys.B.name(),
        CreditRatingStandardAndPoors.B.name(),
        0);

    _totalOrganisations = 3;
  }

  @AfterMethod
  public void tearDown() throws Exception {
    if (_readOnly == false) {
      _orgMaster = null;
      super.tearDown();
    }
  }

  @AfterClass
  public void tearDownClass() throws Exception {
    if (_readOnly) {
      _orgMaster = null;
      super.tearDown();
    }
  }

  @AfterSuite
  public static void closeAfterSuite() {
    DbMasterTestUtils.closeAfterSuite();
  }


  protected void assert101(final OrganisationDocument test) {
    UniqueId uniqueId = UniqueId.of("DbOrg", "101", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageableOrganisation organisation = test.getOrganisation();
    assertNotNull(organisation);
    assertEquals(uniqueId, organisation.getUniqueId());
    assertEquals(ExternalId.of(PROVIDER_SCHEME, "1"), test.getProviderId());
    assertEquals("TestOrganisation101", test.getOrganisation().getObligor().getObligorShortName());
    assertEquals("RED_code_101", test.getOrganisation().getObligor().getObligorREDCode());
    assertEquals("ticker_101", test.getOrganisation().getObligor().getObligorTicker());
    assertEquals("CountryA", test.getOrganisation().getObligor().getCountry());
    assertEquals(Region.AFRICA, test.getOrganisation().getObligor().getRegion());
    assertEquals(Sector.BASICMATERIALS, test.getOrganisation().getObligor().getSector());
    assertEquals(CreditRating.A, test.getOrganisation().getObligor().getCompositeRating());
    assertEquals(CreditRating.A, test.getOrganisation().getObligor().getImpliedRating());
    assertEquals(CreditRatingFitch.A, test.getOrganisation().getObligor().getFitchCreditRating());
    assertEquals(CreditRatingMoodys.A, test.getOrganisation().getObligor().getMoodysCreditRating());
    assertEquals(CreditRatingStandardAndPoors.A, test.getOrganisation().getObligor().getStandardAndPoorsCreditRating());
    assertEquals(false, test.getOrganisation().getObligor().isHasDefaulted());
  }

  protected void assert102(final OrganisationDocument test) {
    UniqueId uniqueId = UniqueId.of("DbOrg", "102", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageableOrganisation organisation = test.getOrganisation();
    assertNotNull(organisation);
    assertEquals(uniqueId, organisation.getUniqueId());
    assertEquals(ExternalId.of(PROVIDER_SCHEME, "2"), test.getProviderId());
    assertEquals("TestOrganisation102", test.getOrganisation().getObligor().getObligorShortName());
    assertEquals("RED_code_102", test.getOrganisation().getObligor().getObligorREDCode());
    assertEquals("ticker_102", test.getOrganisation().getObligor().getObligorTicker());
    assertEquals("CountryB", test.getOrganisation().getObligor().getCountry());
    assertEquals(Region.AFRICA, test.getOrganisation().getObligor().getRegion());
    assertEquals(Sector.BASICMATERIALS, test.getOrganisation().getObligor().getSector());
    assertEquals(CreditRating.A, test.getOrganisation().getObligor().getCompositeRating());
    assertEquals(CreditRating.A, test.getOrganisation().getObligor().getImpliedRating());
    assertEquals(CreditRatingFitch.A, test.getOrganisation().getObligor().getFitchCreditRating());
    assertEquals(CreditRatingMoodys.A, test.getOrganisation().getObligor().getMoodysCreditRating());
    assertEquals(CreditRatingStandardAndPoors.A, test.getOrganisation().getObligor().getStandardAndPoorsCreditRating());
    assertEquals(false, test.getOrganisation().getObligor().isHasDefaulted());
  }

  protected void assert201(final OrganisationDocument test) {
    UniqueId uniqueId = UniqueId.of("DbOrg", "201", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(_version2Instant, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageableOrganisation organisation = test.getOrganisation();
    assertNotNull(organisation);
    assertEquals(uniqueId, organisation.getUniqueId());
    assertEquals(ExternalId.of(PROVIDER_SCHEME, "3"), test.getProviderId());
    assertEquals("TestOrganisation201", test.getOrganisation().getObligor().getObligorShortName());
    assertEquals("RED_code_201", test.getOrganisation().getObligor().getObligorREDCode());
    assertEquals("ticker_201", test.getOrganisation().getObligor().getObligorTicker());
    assertEquals("CountryC", test.getOrganisation().getObligor().getCountry());
    assertEquals(Region.AFRICA, test.getOrganisation().getObligor().getRegion());
    assertEquals(Sector.BASICMATERIALS, test.getOrganisation().getObligor().getSector());
    assertEquals(CreditRating.A, test.getOrganisation().getObligor().getCompositeRating());
    assertEquals(CreditRating.A, test.getOrganisation().getObligor().getImpliedRating());
    assertEquals(CreditRatingFitch.A, test.getOrganisation().getObligor().getFitchCreditRating());
    assertEquals(CreditRatingMoodys.A, test.getOrganisation().getObligor().getMoodysCreditRating());
    assertEquals(CreditRatingStandardAndPoors.A, test.getOrganisation().getObligor().getStandardAndPoorsCreditRating());
    assertEquals(false, test.getOrganisation().getObligor().isHasDefaulted());
  }

  protected void assert202(final OrganisationDocument test) {
    UniqueId uniqueId = UniqueId.of("DbOrg", "201", "1");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version2Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version2Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageableOrganisation organisation = test.getOrganisation();
    assertNotNull(organisation);
    assertEquals(uniqueId, organisation.getUniqueId());
    assertEquals(ExternalId.of(PROVIDER_SCHEME, "3"), test.getProviderId());
    assertEquals("TestOrganisation201", test.getOrganisation().getObligor().getObligorShortName());
    assertEquals("RED_code_201", test.getOrganisation().getObligor().getObligorREDCode());
    assertEquals("ticker_201", test.getOrganisation().getObligor().getObligorTicker());
    assertEquals("CountryD", test.getOrganisation().getObligor().getCountry());
    assertEquals(Region.AFRICA, test.getOrganisation().getObligor().getRegion());
    assertEquals(Sector.BASICMATERIALS, test.getOrganisation().getObligor().getSector());
    assertEquals(CreditRating.B, test.getOrganisation().getObligor().getCompositeRating());
    assertEquals(CreditRating.B, test.getOrganisation().getObligor().getImpliedRating());
    assertEquals(CreditRatingFitch.B, test.getOrganisation().getObligor().getFitchCreditRating());
    assertEquals(CreditRatingMoodys.B, test.getOrganisation().getObligor().getMoodysCreditRating());
    assertEquals(CreditRatingStandardAndPoors.B, test.getOrganisation().getObligor().getStandardAndPoorsCreditRating());
    assertEquals(false, test.getOrganisation().getObligor().isHasDefaulted());
  }

}
