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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.reflect.Array;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.ListComprehension.OngoingDefinitionWithVariable;
import org.neo4j.cypherdsl.core.PatternComprehension.OngoingDefinitionWithPattern;
import org.neo4j.cypherdsl.core.ProcedureCall.OngoingStandaloneCallWithoutArguments;
import org.neo4j.cypherdsl.core.Statement.SingleQuery;
import org.neo4j.cypherdsl.core.support.Neo4jVersion;
import org.neo4j.cypherdsl.core.support.UnsupportedLiteralException;
import org.neo4j.cypherdsl.core.utils.Assertions;

/**
 * The main entry point into the Cypher DSL.
 * The Cypher Builder API is intended for framework usage to produce Cypher statements required for database operations.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Andreas Berger
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Cypher {

	/**
	 * Create a new Node representation with at least one label, the "primary" label. This is required. All other labels
	 * are optional.
	 *
	 * @param primaryLabel     The primary label this node is identified by.
	 * @param additionalLabels Additional labels
	 * @return A new node representation
	 */
	public static Node node(String primaryLabel, String... additionalLabels) {

		return new InternalNodeImpl(primaryLabel, additionalLabels);
	}

	/**
	 * Create a new Node representation with at least one label, the "primary" label. This is required. All other labels
	 * are optional.
	 *
	 * @param primaryLabel     The primary label this node is identified by.
	 * @param additionalLabels Additional labels
	 * @return A new node representation
	 */
	public static Node node(String primaryLabel, List<String> additionalLabels) {

		return new InternalNodeImpl(primaryLabel, additionalLabels.toArray(new String[] {}));
	}

	/**
	 * Create a new Node representation with at least one label, the "primary" label. This is required. All other labels
	 * are optional. This method also takes a map of properties. This allows the returned node object to be used in a
	 * {@code MATCH} or {@code MERGE} statement.
	 *
	 * @param primaryLabel     The primary label this node is identified by.
	 * @param properties       The properties expected to exist on the node.
	 * @param additionalLabels Additional labels
	 * @return A new node representation
	 */
	public static Node node(String primaryLabel, MapExpression properties, String... additionalLabels) {

		return new InternalNodeImpl(null, primaryLabel, properties, additionalLabels);
	}

	/**
	 * @return A node matching any node.
	 */
	public static Node anyNode() {
		return new InternalNodeImpl();
	}

	/**
	 * @return The {@code *} wildcard literal.
	 */
	public static Asterisk asterisk() {
		return Asterisk.INSTANCE;
	}

	/**
	 * @param symbolicName The new symbolic name
	 * @return A node matching any node with the symbolic the given {@code symbolicName}.
	 */
	public static Node anyNode(String symbolicName) {
		return new InternalNodeImpl().named(symbolicName);
	}

	/**
	 * @param symbolicName The new symbolic name
	 * @return A node matching any node with the symbolic the given {@code symbolicName}.
	 */
	public static Node anyNode(SymbolicName symbolicName) {
		return new InternalNodeImpl().named(symbolicName);
	}

	/**
	 * Dereferences a property for a symbolic name, most likely pointing to a property container like a node or a relationship.
	 *
	 * @param containerName The symbolic name of a property container
	 * @param names         The names of the properties to dereference. More than one name does create a nested property
	 *                      like {@code containerName.name1.name2}.
	 * @return A new property
	 */
	public static Property property(String containerName, String... names) {
		return property(name(containerName), names);
	}

	/**
	 * Dereferences a property on a arbitrary expression.
	 *
	 * @param expression The expression that describes some sort of accessible map
	 * @param names      The names of the properties to dereference. More than one name does create a nested property
	 *                   like {@code expression.name1.name2}.
	 * @return A new property.
	 */
	public static Property property(Expression expression, String... names) {
		return Property.create(expression, names);
	}

	/**
	 * Creates a dynamic lookup of a property for a symbolic name, most likely pointing to a property container like a
	 * node or a relationship. A dynamic property will be rendered as {@code p[expression]}.
	 *
	 * @param containerName The symbolic name of a property container
	 * @param lookup        An expression to use as a dynamic lookup for properties of the container with the given name
	 * @return A new property
	 * @since 2021.0.0
	 */
	public static Property property(String containerName, Expression lookup) {
		return property(name(containerName), lookup);
	}

	/**
	 * Creates a dynamic lookup of a property on a arbitrary expression. A dynamic property will be rendered as
	 * {@code p[expression]}.
	 *
	 * @param expression The expression that describes some sort of accessible map
	 * @param lookup     An expression to use as a dynamic lookup for properties of the container the expression resolved to
	 * @return A new property.
	 * @since 2021.0.0
	 */
	public static Property property(Expression expression, Expression lookup) {
		return Property.create(expression, lookup);
	}

	/**
	 * Starts defining a named path by indicating a name.
	 *
	 * @param name The name of the new path
	 * @return An ongoing definition of a named path
	 * @since 1.1
	 */
	public static NamedPath.OngoingDefinitionWithName path(String name) {
		return NamedPath.named(name);
	}

	/**
	 * Starts defining a named path by indicating a name.
	 *
	 * @param name The name of the new path
	 * @return An ongoing definition of a named path
	 * @since 1.1
	 */
	public static NamedPath.OngoingDefinitionWithName path(SymbolicName name) {
		return NamedPath.named(name);
	}

	/**
	 * Starts defining a named path defined by the {@code shortestPath} between a relationship by indicating a name.
	 *
	 * @param name The name of the new shortestPath path
	 * @return An ongoing definition of a named path
	 * @since 1.1.1
	 */
	public static NamedPath.OngoingShortestPathDefinitionWithName shortestPath(String name) {
		return NamedPath.named(name, BuiltInFunctions.Scalars.SHORTEST_PATH);
	}

	/**
	 * Starts defining a named path defined by the {@code shortestPath} between a relationship by indicating a name.
	 *
	 * @param name The name of the new shortestPath path
	 * @return An ongoing definition of a named path
	 * @since 1.1.1
	 */
	public static NamedPath.OngoingShortestPathDefinitionWithName shortestPath(SymbolicName name) {
		return NamedPath.named(name, BuiltInFunctions.Scalars.SHORTEST_PATH);
	}

	/**
	 * Creates a new symbolic name.
	 *
	 * @param value The value of the symbolic name
	 * @return A new symbolic name
	 */
	public static SymbolicName name(String value) {

		return SymbolicName.of(value);
	}

	/**
	 * Creates a new parameter placeholder. Existing $-signs will be removed.
	 *
	 * @param name The name of the parameter, must not be null
	 * @return The new parameter
	 */
	public static Parameter<Object> parameter(String name) {
		return Parameter.create(name);
	}

	/**
	 * Creates a new parameter with the given {@code name} and a value bound to it.
	 * The value can be retrieved from the final statement build.
	 *
	 * @param name The name of the parameter, must not be null
	 * @param value The value of the parameter.
	 * @param <T> Type of the new parameter
	 * @return The new parameter
	 * @since 2021.0.0
	 */
	public static <T>  Parameter<T> parameter(String name, T value) {
		return Parameter.create(name, value);
	}

	/**
	 * Prepares an optional match statement.
	 *
	 * @param pattern The patterns to match
	 * @return An ongoing match that is used to specify an optional where and a required return clause
	 */
	public static StatementBuilder.OngoingReadingWithoutWhere optionalMatch(PatternElement... pattern) {

		return Statement.builder().optionalMatch(pattern);
	}

	/**
	 * Starts building a statement based on a match clause. Use {@link Cypher#node(String, String...)} and related to
	 * retrieve a node or a relationship, which both are pattern elements.
	 *
	 * @param pattern The patterns to match
	 * @return An ongoing match that is used to specify an optional where and a required return clause
	 */
	public static StatementBuilder.OngoingReadingWithoutWhere match(PatternElement... pattern) {

		return Statement.builder().match(pattern);
	}

	/**
	 * Starts building a statement based on a match clause. Use {@link Cypher#node(String, String...)} and related to
	 * retrieve a node or a relationship, which both are pattern elements.
	 *
	 * @param optional A flag whether the {@code MATCH} clause includes the {@code OPTIONAL} keyword.
	 * @param pattern  The patterns to match
	 * @return An ongoing match that is used to specify an optional where and a required return clause
	 * @since 2020.1.3
	 */
	public static StatementBuilder.OngoingReadingWithoutWhere match(boolean optional, PatternElement... pattern) {

		return Statement.builder().match(optional, pattern);
	}

	/**
	 * Starts building a statement based on a {@code CREATE} clause.
	 *
	 * @param pattern The patterns to create
	 * @return An ongoing {@code CREATE} that can be used to specify {@code WITH} and {@code RETURNING} etc.
	 */
	public static StatementBuilder.OngoingUpdate create(
		PatternElement... pattern) {

		return Statement.builder().create(pattern);
	}

	/**
	 * Starts a statement with a leading {@code WITH}. Those are useful for passing on lists of various type that
	 * can be unwound later on etc. A leading {@code WITH} cannot be used with patterns obviously and needs its
	 * arguments to have an alias.
	 *
	 * @param variables One ore more variables.
	 * @return An ongoing with clause.
	 * @since 2020.1.2
	 */
	public static StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere with(String... variables) {

		return Statement.builder().with(variables);
	}

	/**
	 * Starts a statement with a leading {@code WITH}. Those are useful for passing on lists of various type that
	 * can be unwound later on etc. A leading {@code WITH} cannot be used with patterns obviously and needs its
	 * arguments to have an alias.
	 *
	 * @param variables One ore more variables.
	 * @return An ongoing with clause.
	 * @since 2020.1.2
	 */
	public static StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere with(Named... variables) {

		return Statement.builder().with(variables);
	}

	/**
	 * Starts a statement with a leading {@code WITH}. Those are useful for passing on lists of various type that
	 * can be unwound later on etc. A leading {@code WITH} cannot be used with patterns obviously and needs its
	 * arguments to have an alias.
	 * <p>
	 * This method takes both aliased and non-aliased expression. The later will produce only valid Cypher when used in
	 * combination with a correlated subquery via {@link Cypher#call(Statement)}.
	 *
	 * @param expressions One ore more aliased expressions.
	 * @return An ongoing with clause.
	 */
	public static StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere with(Expression... expressions) {

		return Statement.builder().with(expressions);
	}

	/**
	 * Starts building a statement based on a {@code MERGE} clause.
	 *
	 * @param pattern The patterns to merge
	 * @return An ongoing {@code MERGE} that can be used to specify {@code WITH} and {@code RETURNING} etc.
	 */
	public static StatementBuilder.OngoingMerge merge(
		PatternElement... pattern) {

		return Statement.builder().merge(pattern);
	}

	/**
	 * Starts building a statement starting with an {@code UNWIND} clause. The expression needs to be an expression
	 * evaluating to a list, otherwise the query will fail.
	 *
	 * @param expression The expression to unwind
	 * @return An ongoing {@code UNWIND}.
	 */
	public static StatementBuilder.OngoingUnwind unwind(Expression expression) {

		return Statement.builder().unwind(expression);
	}

	/**
	 * Starts building a statement starting with an {@code UNWIND} clause. The expressions passed will be turned into a
	 * list expression
	 *
	 * @param expressions expressions to unwind
	 * @return a new instance of {@link StatementBuilder.OngoingUnwind}
	 */
	public static StatementBuilder.OngoingUnwind unwind(Expression... expressions) {

		return Statement.builder().unwind(Cypher.listOf(expressions));
	}

	/**
	 * Creates a new {@link SortItem} to be used as part of an {@link Order}.
	 *
	 * @param expression The expression by which things should be sorted
	 * @return A sort item, providing means to specify ascending or descending order
	 */
	public static SortItem sort(Expression expression) {

		return SortItem.create(expression, null);
	}

	/**
	 * Creates a map of expression from a list of key/value pairs.
	 *
	 * @param keysAndValues A list of key and values. Must be an even number, with alternating {@link String} and {@link Expression}
	 * @return A new map expression.
	 */
	public static MapExpression mapOf(Object... keysAndValues) {

		return MapExpression.create(keysAndValues);
	}

	/**
	 * Creates a map of expression from a Java Map.
	 *
	 * @param map A map to be turned into a MapExpression
	 * @return A new map expression.
	 * @since 2021.1.0
	 */
	public static MapExpression asExpression(Map<String, Object> map) {

		return MapExpression.create(map);
	}

	/**
	 * Creates a {@link ListExpression list-expression} from several expressions.
	 *
	 * @param expressions expressions to get combined into a list
	 * @return a new instance of {@link ListExpression}
	 */
	public static ListExpression listOf(Expression... expressions) {

		return ListExpression.create(expressions);
	}

	/**
	 * Creates a new {@link Literal Literal&lt;?&gt;} from the given {@code object}.
	 *
	 * @param object the object to represent.
	 * @param <T>    The type of the literal returned
	 * @return a new {@link Literal Literal&lt;?&gt;}.
	 * @throws IllegalArgumentException when the object cannot be represented as a literal
	 */
	@SuppressWarnings("unchecked")
	public static <T> Literal<T> literalOf(Object object) {

		if (object == null) {
			return (Literal<T>) NullLiteral.INSTANCE;
		}
		if (object instanceof Literal<?>) {
			return (Literal<T>) object;
		}
		if (object instanceof CharSequence) {
			return (Literal<T>) new StringLiteral((CharSequence) object);
		}
		if (object instanceof Character) {
			return (Literal<T>) new StringLiteral(String.valueOf(object));
		}
		if (object instanceof Number) {
			return (Literal<T>) new NumberLiteral((Number) object);
		}
		if (object instanceof TemporalAccessor) {
			return (Literal<T>) new TemporalLiteral((TemporalAccessor) object);
		}
		if (object instanceof Iterable || object.getClass().isArray()) {
			List<Literal<?>> elements = new ArrayList<>();
			Consumer<Object> handleElement = element -> {
				if (element instanceof Literal) {
					elements.add((Literal<?>) element);
				} else {
					try {
						elements.add(Cypher.literalOf(element));
					} catch (UnsupportedLiteralException e) {
						throw new UnsupportedLiteralException("Unsupported literal type in iterable.", element);
					}
				}
			};
			if (object.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(object); i++) {
					handleElement.accept(Array.get(object, i));
				}
			} else {
				((Iterable<?>) object).forEach(handleElement);
			}

			ListLiteral listLiteral = new ListLiteral(elements);
			return (Literal<T>) listLiteral;
		}
		if (object instanceof Boolean) {
			return (Literal<T>) BooleanLiteral.of((Boolean) object);
		}
		throw new UnsupportedLiteralException(object);
	}

	/**
	 * @return The {@literal true} literal.
	 */
	public static Literal<Boolean> literalTrue() {
		return BooleanLiteral.TRUE;
	}

	/**
	 * @return The {@literal false} literal.
	 */
	public static Literal<Boolean> literalFalse() {
		return BooleanLiteral.FALSE;
	}

	/**
	 * Creates a {@code UNION} statement from several other statements. No checks are applied for matching return types.
	 *
	 * @param statements the statements to union.
	 * @return A union statement.
	 */
	public static Statement union(Statement... statements) {
		return unionImpl(false, statements);
	}

	/**
	 * Creates a {@code UNION ALL} statement from several other statements. No checks are applied for matching return types.
	 *
	 * @param statements the statements to union.
	 * @return A union statement.
	 */
	public static Statement unionAll(Statement... statements) {
		return unionImpl(true, statements);
	}

	/**
	 * A {@literal RETURN} statement without a previous match.
	 *
	 * @param expressions The expressions to return
	 * @return A buildable statement
	 * @since 1.0.1
	 */
	public static StatementBuilder.OngoingReadingAndReturn returning(Expression... expressions) {
		return Statement.builder().returning(expressions);
	}

	/**
	 * Creates a list comprehension starting with a {@link Relationship} or a {@link RelationshipChain chain of relationships}.
	 *
	 * @param relationshipPattern The relationship pattern on which the new list comprehension is based on.
	 * @return An ongoing definition.
	 * @since 2020.0.0
	 */
	public static OngoingDefinitionWithPattern listBasedOn(RelationshipPattern relationshipPattern) {
		return PatternComprehension.basedOn(relationshipPattern);
	}

	/**
	 * Creates a list comprehension starting with a {@link NamedPath named path}.
	 *
	 * @param namedPath The named path on which the new list comprehension is based on.
	 * @return An ongoing definition.
	 * @since 2020.1.1
	 */
	public static OngoingDefinitionWithPattern listBasedOn(NamedPath namedPath) {
		return PatternComprehension.basedOn(namedPath);
	}

	/**
	 * Starts defining a {@link ListComprehension list comprehension}.
	 *
	 * @param variable The variable to which each element of the list is assigned.
	 * @return An ongoing definition of a list comprehension
	 * @since 1.0.1
	 */
	public static OngoingDefinitionWithVariable listWith(SymbolicName variable) {
		return ListComprehension.with(variable);
	}

	/**
	 * Escapes and quotes the {@code unquotedString} for safe usage in Neo4j-Browser and Shell.
	 *
	 * @param unquotedString An unquoted string
	 * @return A quoted string with special chars escaped.
	 */
	public static String quote(String unquotedString) {
		return literalOf(unquotedString).asString();
	}

	/**
	 * @return generic case expression start
	 */
	public static Case caseExpression() {
		return Case.create();
	}

	/**
	 * @param expression initial expression for the simple case statement
	 * @return simple case expression start
	 */
	public static Case caseExpression(Expression expression) {
		return Case.create(expression);
	}

	/**
	 * Starts defining a procedure call of the procedure with the given {@literal procedureName}. That
	 * procedure name might be fully qualified - that is, including a namespace - or just a simple name.
	 *
	 * @param procedureName The procedure name of the procedure to call. Might be fully qualified.
	 * @return An ongoing definition of a call
	 */
	public static OngoingStandaloneCallWithoutArguments call(String procedureName) {

		Assertions.hasText(procedureName, "The procedure name must not be null or empty.");
		return call(procedureName.split("\\."));
	}

	/**
	 * Starts defining a procedure call of the procedure with the given qualified name.
	 *
	 * @param namespaceAndProcedure The procedure name of the procedure to call.
	 * @return An ongoing definition of a call
	 */
	public static OngoingStandaloneCallWithoutArguments call(String... namespaceAndProcedure) {
		return Statement.call(namespaceAndProcedure);
	}

	/**
	 * Starts building a statement based on one subquery.
	 *
	 * @param subquery The statement representing the subquery
	 * @return A new ongoing read without any further conditions or returns.
	 * @neo4j.version 4.0.0
	 * @see ExposesSubqueryCall#call(Statement)
	 * @since 2020.1.2
	 */
	@Neo4jVersion(minimum = "4.0.0")
	public static StatementBuilder.OngoingReadingWithoutWhere call(Statement subquery) {
		return Statement.builder().call(subquery);
	}

	/**
	 * Creates a closed range with given boundaries.
	 *
	 * @param targetExpression The target expression for the range
	 * @param start            The inclusive start
	 * @param end              The exclusive end
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static Expression subList(Expression targetExpression, Integer start, Integer end) {

		return ListOperator.subList(targetExpression, Cypher.literalOf(start), Cypher.literalOf(end));
	}

	/**
	 * Creates a closed range with given boundaries.
	 *
	 * @param targetExpression The target expression for the range
	 * @param start            The inclusive start
	 * @param end              The exclusive end
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static Expression subList(Expression targetExpression, Expression start, Expression end) {

		return ListOperator.subList(targetExpression, start, end);
	}

	/**
	 * Creates an open range starting at {@code start}.
	 *
	 * @param targetExpression The target expression for the range
	 * @param start            The inclusive start
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static Expression subListFrom(Expression targetExpression, Integer start) {

		return ListOperator.subListFrom(targetExpression, Cypher.literalOf(start));
	}

	/**
	 * Creates an open range starting at {@code start}.
	 *
	 * @param targetExpression The target expression for the range
	 * @param start            The inclusive start
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static Expression subListFrom(Expression targetExpression, Expression start) {

		return ListOperator.subListFrom(targetExpression, start);
	}

	/**
	 * Creates an open range starting at {@code start}.
	 *
	 * @param targetExpression The target expression for the range
	 * @param end              The exclusive end
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static Expression subListUntil(Expression targetExpression, Integer end) {

		return ListOperator.subListUntil(targetExpression, Cypher.literalOf(end));
	}

	/**
	 * Creates an open range starting at {@code start}.
	 *
	 * @param targetExpression The target expression for the range
	 * @param end              The exclusive end
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static Expression subListUntil(Expression targetExpression, Expression end) {

		return ListOperator.subListUntil(targetExpression, end);
	}

	/**
	 * Creates a single valued range at {@code index}.
	 *
	 * @param targetExpression The target expression for the range
	 * @param index            The index of the range
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static ListOperator valueAt(Expression targetExpression, Integer index) {

		return ListOperator.valueAt(targetExpression, Cypher.literalOf(index));
	}

	/**
	 * Creates a single valued range at {@code index}.
	 *
	 * @param targetExpression The target expression for the range
	 * @param index            The index of the range
	 * @return A range literal.
	 * @since 2020.1.0
	 */
	public static ListOperator valueAt(Expression targetExpression, Expression index) {

		return ListOperator.valueAt(targetExpression, index);
	}

	/**
	 * Creates an expression from a raw string fragment. No validation is performed on it. If it is used as expression,
	 * you must make sure to define something that works as expression.
	 * <p>
	 * This method expects exactly one placeholder in the form of {@literal $E} for any argument passed with {@code mixedArgs}.
	 * <p>
	 * To use exactly the term {@literal $E} escape it like this: {@literal \$E}
	 *
	 * @param format A raw Cypher string
	 * @param mixedArgs Args to the Cypher string
	 * @return An expression to reuse with the builder.
	 * @since 2021.0.2
	 */
	public static Expression raw(String format, Object... mixedArgs) {

		return RawLiteral.create(format, mixedArgs);
	}

	private static Statement unionImpl(boolean unionAll, Statement... statements) {

		Assertions.isTrue(statements != null && statements.length >= 2, "At least two statements are required!");

		int i = 0;
		UnionQuery existingUnionQuery = null;
		if (statements[0] instanceof UnionQuery) {
			existingUnionQuery = (UnionQuery) statements[0];
			Assertions.isTrue(existingUnionQuery.isAll() == unionAll, "Cannot mix union and union all!");
			i = 1;
		}

		List<SingleQuery> listOfQueries = new ArrayList<>();
		do {
			Assertions.isInstanceOf(SingleQuery.class, statements[i], "Can only union single queries!");
			listOfQueries.add((SingleQuery) statements[i]);
		} while (++i < statements.length);

		if (existingUnionQuery == null) {
			return UnionQuery.create(unionAll, listOfQueries);
		} else {
			return existingUnionQuery.addAdditionalQueries(listOfQueries);
		}
	}

	/**
	 * Not to be instantiated.
	 */
	private Cypher() {
	}
}
