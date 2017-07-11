package hr.fer.zemris.bf.utils;

import java.util.List;
import java.util.StringJoiner;

import hr.fer.zemris.bf.model.BinaryOperatorNode;
import hr.fer.zemris.bf.model.ConstantNode;
import hr.fer.zemris.bf.model.Node;
import hr.fer.zemris.bf.model.NodeVisitor;
import hr.fer.zemris.bf.model.UnaryOperatorNode;
import hr.fer.zemris.bf.model.VariableNode;
import hr.fer.zemris.bf.parser.Parser;

/**
 * Razred koji implementira sučelje {@link NodeVisitor}. Ovaj razred predstavlja
 * konkretnog posjetitelja. zadaća ovog posjetitelja je spojiti parsirani
 * logički izraz natrag u primjerak razreda {@link String}. Ovaj primjerak
 * razreda {@link String} moguće je ponovno parsirati primjerkom razreda
 * {@link Parser}. Razred nudi jednu metodu za dohvat igrađenog primjerka
 * razreda {@link String}
 * 
 * @see NodeVisitor
 * @see Parser
 * 
 * @author Davor Češljaš
 */
public class NodeAsExpressionPrinter implements NodeVisitor {

	/**
	 * Članska varijabla koja predstavlja primjerak razreda {@link StringJoiner}
	 * kojim se čvorovi spajaju natrag u primjerak razreda {@link String}
	 */
	private StringJoiner expressionJoiner = new StringJoiner(" ");

	@Override
	public void visit(ConstantNode node) {
		expressionJoiner.add(String.valueOf(node.getValue()));
	}

	@Override
	public void visit(VariableNode node) {
		expressionJoiner.add(node.toString());
	}

	@Override
	public void visit(UnaryOperatorNode node) {
		expressionJoiner.add(node.toString());
		node.getChild().accept(this);
	}

	@Override
	public void visit(BinaryOperatorNode node) {
		String nodeName = node.toString();
		List<Node> children = node.getChildren();
		for (int i = 0, len = children.size(); i < len; i++) {
			Node child = children.get(i);
			child.accept(this);
			if (i != len - 1) {
				expressionJoiner.add(nodeName);
			}
		}
	}

	/**
	 * Metoda koja dohvaća ponovno izgrađeni booleov izraz
	 *
	 * @return ponovno izgrađeni booleov izraz
	 */
	public String getExpressionAsString() {
		return expressionJoiner.toString();
	}
}
