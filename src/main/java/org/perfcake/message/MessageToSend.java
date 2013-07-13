/*
 * Copyright 2010-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.perfcake.message;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.perfcake.util.Utils;
import org.perfcake.util.properties.DefaultPropertyGetter;

/**
 * 
 * @author Lucie Fabriková <lucie.fabrikova@gmail.com>
 * @author Martin Večeřa <marvenec@gmail.com>
 */
public class MessageToSend {
   private static final String propertyPattern = "[^\\\\]#\\{([^#\\{:]+)(:[^#\\{:]*)?}";

   private Message message;
   private long multiplicity;
   private String validatorId;// may be null
   private Matcher matcher;

   public Matcher getMatcher() {
      return matcher;
   }

   public MessageToSend() {

   }

   public MessageToSend(Message message, long multiplicity, String validatorId) {
      setMessage(message);
      this.multiplicity = multiplicity;
      this.validatorId = validatorId;
   }

   public Message getMessage() {
      return message;
   }
   
   public Message getFilteredMessage(Properties props) {
      if (getMatcher() != null) {
         Message m = MessageFactory.getMessage();
         String text = this.getMessage().getPayload().toString();
         text = Utils.filterProperties(text, getMatcher(), new DefaultPropertyGetter(props));

         m.setPayload(text);
         
         return m;
      } else {
         return message;
      }
   }

   public void setMessage(Message message) {
      this.message = message;
      
      this.matcher = null;
      
      // find out if there are any attributes in the text message to be replaced
      if (message.getPayload() instanceof String) {
         String filteredString = (String) message.getPayload();
         Matcher matcher = Pattern.compile(propertyPattern).matcher(filteredString);
         if (matcher.find()) {
            this.matcher = matcher;
         }
      }

   }

   public Long getMultiplicity() {
      return multiplicity;
   }

   public void setMultiplicity(Long multiplicity) {
      this.multiplicity = multiplicity;
   }

   public String getValidatorId() {
      return validatorId;
   }

   public void setValidatorId(String validatorId) {
      this.validatorId = validatorId;
   }

}