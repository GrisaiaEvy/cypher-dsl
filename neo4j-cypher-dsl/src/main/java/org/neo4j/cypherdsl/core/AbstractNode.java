/*
 * Copyright (c) 2019-2021 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypherdsl.core;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.INTERNAL;

/**
 * Abstract base class for the {@link NodeImpl node implementation} to avoid default interface methods to be overridable
 * in inheritors.
 *
 * @author Michael J. Simons
 * @since 2021.1.0
 */
@API(status = INTERNAL, since = "2021.1.0")
abstract class AbstractNode extends AbstractPropertyContainer implements Node {

	@Override
	public final Condition hasLabels(String... labelsToQuery) {
		return HasLabelCondition.create(this.getSymbolicName()
						.orElseThrow(() -> new IllegalStateException("Cannot query a node without a symbolic name.")),
				labelsToQuery);
	}

	@Override
	public final Condition isEqualTo(Node otherNode) {

		return this.getRequiredSymbolicName().isEqualTo(otherNode.getRequiredSymbolicName());
	}

	@Override
	public final Condition isNotEqualTo(Node otherNode) {

		return this.getRequiredSymbolicName().isNotEqualTo(otherNode.getRequiredSymbolicName());
	}

	@Override
	public final Condition isNull() {

		return this.getRequiredSymbolicName().isNull();
	}

	@Override
	public final Condition isNotNull() {

		return this.getRequiredSymbolicName().isNotNull();
	}

	@Override
	public final SortItem descending() {

		return this.getRequiredSymbolicName().descending();
	}

	@Override
	public final SortItem ascending() {

		return this.getRequiredSymbolicName().ascending();
	}

	@Override
	public final AliasedExpression as(String alias) {

		return this.getRequiredSymbolicName().as(alias);
	}

	@Override
	public final FunctionInvocation internalId() {
		return Functions.id(this);
	}

	@Override
	public final FunctionInvocation labels() {
		return Functions.labels(this);
	}

	@Override
	public final Relationship relationshipTo(Node other, String... types) {
		return new RelationshipImpl(null, this, Relationship.Direction.LTR, other, types);
	}

	@Override
	public final Relationship relationshipFrom(Node other, String... types) {
		return new RelationshipImpl(null, this, Relationship.Direction.RTL, other, types);
	}

	@Override
	public final Relationship relationshipBetween(Node other, String... types) {
		return new RelationshipImpl(null, this, Relationship.Direction.UNI, other, types);
	}
}
