/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.security;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.time.calendar.LocalDateTime;

import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeFieldContainer;
import org.fudgemsg.mapping.FudgeDeserializationContext;
import org.fudgemsg.mapping.FudgeSerializationContext;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.engine.security.DefaultSecurity;
import com.opengamma.engine.security.Security;
import com.opengamma.financial.fudgemsg.FinancialFudgeContextConfiguration;
import com.opengamma.financial.security.swap.SwapLeg;
import com.opengamma.financial.security.swap.SwapSecurity;

public class FudgeSecurityEncodingTest extends SecurityTestCase {

  private static final Logger s_logger = LoggerFactory.getLogger(FudgeSecurityEncodingTest.class);

  private static final FudgeContext s_fudgeContext = new FudgeContext();

  static {
    final FinancialFudgeContextConfiguration context = new FinancialFudgeContextConfiguration();
    context.setRegionRepository(getRegionRepository());
    s_fudgeContext.setConfiguration(context);
  }

  @Override
  protected <T extends DefaultSecurity> void testSecurity(Class<T> securityClass, T security) {
    final FudgeSerializationContext context = new FudgeSerializationContext(s_fudgeContext);
    FudgeFieldContainer msg = context.objectToFudgeMsg(security);
    s_logger.debug("Security {}", security);
    s_logger.debug("Encoded to {}", security);
    final byte[] bytes = s_fudgeContext.toByteArray(msg);
    msg = s_fudgeContext.deserialize(bytes).getMessage();
    s_logger.debug("Serialised to to {}", msg);
    final Security decoded = s_fudgeContext.fromFudgeMsg(securityClass, msg);
    s_logger.debug("Decoded to {}", decoded);
    if (!security.equals(decoded)) {
      s_logger.warn("Expected {}", security);
      s_logger.warn("Received {}", decoded);
      fail();
    }
  }

  // Temporary measure - the SecurityDocument object should be proto generated
  @Test
  public void testSecurityDocument () {
    final SecurityDocument securityDocument = new SecurityDocument ();
    final List<SwapLeg> legs = getTestObjects (SwapLeg.class, null);
    final SwapSecurity swap = new SwapSecurity (new DateTimeWithZone (LocalDateTime.nowSystemClock ()), new DateTimeWithZone (LocalDateTime.nowSystemClock ()),new DateTimeWithZone (LocalDateTime.nowSystemClock ()), "foo", legs.get (0), legs.get (1));
    securityDocument.setSecurity(swap);
    final FudgeSerializationContext scontext = new FudgeSerializationContext(s_fudgeContext);
    final FudgeFieldContainer message = scontext.objectToFudgeMsg(securityDocument);
    s_logger.debug("Message {}", message);
    final FudgeDeserializationContext dcontext = new FudgeDeserializationContext(s_fudgeContext);
    final Object object = dcontext.fudgeMsgToObject(message);
    s_logger.debug("Object class {}", object.getClass());
    assertTrue(object instanceof SecurityDocument);
    final SecurityDocument returned = (SecurityDocument) object;
    s_logger.debug("Security class {}", returned.getSecurity().getClass());
    assertTrue(returned.getSecurity() instanceof SwapSecurity);
  }

}
