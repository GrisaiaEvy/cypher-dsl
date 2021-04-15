package org.neo4j.cypherdsl.codegen.sdn6.models.valid.enums_and_inner_classes;

import java.util.List;
import javax.annotation.Generated;
import org.neo4j.cypherdsl.core.MapExpression;
import org.neo4j.cypherdsl.core.NodeBase;
import org.neo4j.cypherdsl.core.NodeLabel;
import org.neo4j.cypherdsl.core.Properties;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.cypherdsl.core.SymbolicName;

@Generated(
	value = "org.neo4j.cypherdsl.codegen.core.NodeImplBuilder",
	date = "2019-09-21T21:21:00+01:00",
	comments = "This class is generated by the Neo4j Cypher-DSL. All changes to it will be lost after regeneration."
)
public final class ConnectorTransport_ extends NodeBase<ConnectorTransport_> {
	public static final ConnectorTransport_ CONNECTOR_TRANSPORT = new ConnectorTransport_();

	public final Property VALUE = this.property("value");

	public final Property OTHER_ENUM = this.property("otherEnum");

	public final Property INNER_INNER_CLASS = this.property("innerInnerClass");

	public ConnectorTransport_() {
		super("Transport");
	}

	private ConnectorTransport_(SymbolicName symbolicName, List<NodeLabel> labels,
		Properties properties) {
		super(symbolicName, labels, properties);
	}

	@Override
	public ConnectorTransport_ named(SymbolicName newSymbolicName) {
		return new ConnectorTransport_(newSymbolicName, getLabels(), getProperties());
	}

	@Override
	public ConnectorTransport_ withProperties(MapExpression newProperties) {
		return new ConnectorTransport_(getSymbolicName().orElse(null), getLabels(), Properties.create(newProperties));
	}
}