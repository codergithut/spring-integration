/*
 * Copyright 2002-2017 the original author or authors.
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
 */

package org.springframework.integration.config;

import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Router.
 *
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 */
public class RouterFactoryBean extends AbstractStandardMessageHandlerFactoryBean {

	private volatile Map<String, String> channelMappings;

	private volatile MessageChannel defaultOutputChannel;

	private volatile String defaultOutputChannelName;

	private volatile Boolean resolutionRequired;

	private volatile Boolean applySequence;

	private volatile Boolean ignoreSendFailures;

	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		this.defaultOutputChannel = defaultOutputChannel;
	}

	public void setDefaultOutputChannelName(String defaultOutputChannelName) {
		this.defaultOutputChannelName = defaultOutputChannelName;
	}

	public void setResolutionRequired(Boolean resolutionRequired) {
		this.resolutionRequired = resolutionRequired;
	}

	public void setApplySequence(Boolean applySequence) {
		this.applySequence = applySequence;
	}

	public void setIgnoreSendFailures(Boolean ignoreSendFailures) {
		this.ignoreSendFailures = ignoreSendFailures;
	}

	public void setChannelMappings(Map<String, String> channelMappings) {
		this.channelMappings = channelMappings;
	}

	@Override
	protected MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		Assert.notNull(targetObject, "target object must not be null");
		AbstractMessageRouter router = this.extractTypeIfPossible(targetObject, AbstractMessageRouter.class);
		if (router == null) {
			if (targetObject instanceof MessageHandler && this.noRouterAttributesProvided()
					&& this.methodIsHandleMessageOrEmpty(targetMethodName)) {
				return (MessageHandler) targetObject;
			}
			router = this.createMethodInvokingRouter(targetObject, targetMethodName);
			this.configureRouter(router);
		}
		else {
			Assert.isTrue(!StringUtils.hasText(targetMethodName), "target method should not be provided when the target "
					+ "object is an implementation of AbstractMessageRouter");
			this.configureRouter(router);
			if (targetObject instanceof MessageHandler) {
				return (MessageHandler) targetObject;
			}
		}
		return router;
	}

	@Override
	protected MessageHandler createExpressionEvaluatingHandler(Expression expression) {
		return this.configureRouter(new ExpressionEvaluatingRouter(expression));
	}

	protected AbstractMappingMessageRouter createMethodInvokingRouter(Object targetObject, String targetMethodName) {
		return (StringUtils.hasText(targetMethodName))
				? new MethodInvokingRouter(targetObject, targetMethodName)
				: new MethodInvokingRouter(targetObject);
	}

	protected AbstractMessageRouter configureRouter(AbstractMessageRouter router) {
		if (this.defaultOutputChannel != null) {
			router.setDefaultOutputChannel(this.defaultOutputChannel);
		}
		if (this.defaultOutputChannelName != null) {
			router.setDefaultOutputChannelName(this.defaultOutputChannelName);
		}
		if (getSendTimeout() != null) {
			router.setSendTimeout(getSendTimeout());
		}
		if (this.applySequence != null) {
			router.setApplySequence(this.applySequence);
		}
		if (this.ignoreSendFailures != null) {
			router.setIgnoreSendFailures(this.ignoreSendFailures);
		}
		if (router instanceof AbstractMappingMessageRouter) {
			this.configureMappingRouter((AbstractMappingMessageRouter) router);
		}
		return router;
	}

	protected void configureMappingRouter(AbstractMappingMessageRouter router) {
		if (this.channelMappings != null) {
			router.setChannelMappings(this.channelMappings);
		}
		if (this.resolutionRequired != null) {
			router.setResolutionRequired(this.resolutionRequired);
		}
	}

	@Override
	protected boolean canBeUsedDirect(AbstractMessageProducingHandler handler) {
		return noRouterAttributesProvided();
	}

	protected boolean noRouterAttributesProvided() {
		return this.channelMappings == null && this.defaultOutputChannel == null
				&& getSendTimeout() == null && this.resolutionRequired == null && this.applySequence == null
				&& this.ignoreSendFailures == null;
	}

	@Override
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return AbstractMessageRouter.class;
	}

}
