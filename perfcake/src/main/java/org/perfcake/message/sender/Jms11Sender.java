/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.message.sender;

import org.perfcake.PerfCakeException;
import org.perfcake.reporting.MeasurementUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Properties;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Sends messages via JMS.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 * @author <a href="mailto:baluchw@gmail.com">Marek Baluch</a>
 */
public class Jms11Sender extends AbstractJmsSender {

   /**
    * The sender's logger.
    */
   private static final Logger log = LogManager.getLogger(Jms11Sender.class);

   /**
    * JMS connection.
    */
   protected Connection connection;

   /**
    * JMS session.
    */
   protected Session session;

   /**
    * JMS destination where the messages are send.
    */
   protected Destination destination;

   /**
    * JMS destination sender.
    */
   protected MessageProducer sender;

   /**
    * Creates a new instance of Jms11Sender.
    */
   public Jms11Sender() {
      super();
   }

   @Override
   public void doInit(final Properties messageAttributes) throws PerfCakeException {
      try {
         final Properties ctxProps = new Properties();
         if (jndiUrl != null) {
            ctxProps.setProperty(Context.PROVIDER_URL, jndiUrl);
         }
         if (jndiContextFactory != null) {
            ctxProps.setProperty(Context.INITIAL_CONTEXT_FACTORY, jndiContextFactory);
         }
         if (jndiSecurityPrincipal != null) {
            ctxProps.setProperty(Context.SECURITY_PRINCIPAL, jndiSecurityPrincipal);
         }
         if (jndiSecurityCredentials != null) {
            ctxProps.setProperty(Context.SECURITY_CREDENTIALS, jndiSecurityCredentials);
         }

         if (ctxProps.isEmpty()) {
            ctx = new InitialContext();
         } else {
            ctx = new InitialContext(ctxProps);
         }

         qcf = (ConnectionFactory) ctx.lookup(connectionFactory);
         if (checkCredentials(username, password)) {
            connection = qcf.createConnection(username, password);
         } else {
            connection = qcf.createConnection();
         }
         destination = (Destination) ctx.lookup(safeGetTarget(messageAttributes));
         if (replyTo != null && !"".equals(replyTo)) {
            replyToDestination = (Destination) ctx.lookup(replyTo);
         }
         session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
         connection.start();
         sender = session.createProducer(destination);
         sender.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
      } catch (JMSException | NamingException | RuntimeException e) {
         throw new PerfCakeException(e);
      }
   }

   @Override
   public void doClose() throws PerfCakeException {
      try {
         try {
            if (sender != null) {
               sender.close();
            }
         } finally {
            try {
               if (transacted) {
                  session.commit();
               }
            } finally {
               try {
                  if (session != null) {
                     session.close();
                  }
               } finally {
                  try {
                     if (connection != null) {
                        connection.close();
                     }
                  } finally {
                     if (ctx != null) {
                        ctx.close();
                     }
                  }
               }
            }
         }

      } catch (JMSException | NamingException e) {
         throw new PerfCakeException(e);
      }
   }

   @SuppressWarnings("Duplicates") // false positive with JmsSender
   @Override
   public void preSend(final org.perfcake.message.Message message, final Properties messageAttributes) throws Exception {
      super.preSend(message, messageAttributes);
      switch (messageType) {
         case STRING:
            mess = session.createTextMessage((String) message.getPayload());
            break;
         case BYTEARRAY:
            final BytesMessage bytesMessage = session.createBytesMessage();
            bytesMessage.writeUTF((String) message.getPayload());
            mess = bytesMessage;
            break;
         case OBJECT:
            mess = session.createObjectMessage(message.getPayload());
            break;
      }
      setMessageProperties(message, messageAttributes);
   }

   @Override
   public Serializable doSend(final org.perfcake.message.Message message, final MeasurementUnit measurementUnit) throws Exception {
      if (log.isDebugEnabled()) {
         log.debug("Sending a message: " + message.getPayload().toString());
      }
      try {
         sender.send(mess);
      } catch (final JMSException e) {
         throw new PerfCakeException("JMS Message cannot be sent", e);
      }

      return null;
   }
}
