package com.springone.myrestaurants.domain;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.*;

/**
 * @author Michael Hunger
 * @since 02.10.2010
 */
@Configurable
public class TopRatedRestaurantFinder {

    @Autowired
    Neo4jTemplate template;

    private static final int MAXIMUM_DEPTH = 5;
    public Collection<RatedRestaurant> getTopNRatedRestaurants(final UserAccount user, final int n) {
        final CalculateRatingPredicate calculateRatingPredicate = new CalculateRatingPredicate();
        final Node userNode=user.getPersistentState();
        final TraversalDescription traversalDescription = new TraversalDescriptionImpl()
                .order(Traversal.postorderBreadthFirst())
                .evaluator(Evaluators.toDepth(MAXIMUM_DEPTH))
                .evaluator(calculateRatingPredicate)
                .relationships(DynamicRelationshipType.withName("friends"));
        final Traverser traverser = traversalDescription.traverse(userNode);
        final Iterator<Node> it = traverser.nodes().iterator();
        while (it.hasNext()) {
            it.next();
        }
        return calculateRatingPredicate.getRecommendedRestaurants(n);
    }

    private class CalculateRatingPredicate implements Evaluator {

        class AggregatedRecommendation implements Comparable<AggregatedRecommendation> {
            private Node restaurant;
            private Collection<Relationship> recommendations=new HashSet<Relationship>();
            double stars;
            double sum;

            public AggregatedRecommendation(final Node restaurant) {
                this.restaurant = restaurant;
            }

            @Override
            public int compareTo(final AggregatedRecommendation o) {
                return stars < o.stars ? 1 : stars == o.stars ? 0 : -1;
            }

            public void add(final Relationship recommendation, final int distance) {
                if (recommendations.add(recommendation)) {
                    final Integer userStars = (Integer)recommendation.getProperty("stars", 0);
                    sum += userStars * Math.pow(0.5D,distance);
                    stars = sum / recommendations.size();
                }
            }

            private RatedRestaurant toRatedRestaurant(final CalculateRatingPredicate calculateRatingPredicate) {
                final RatedRestaurant ratedRestaurant = new RatedRestaurant(template.load(restaurant, Restaurant.class));
                for (final Relationship recommendation : recommendations) {
                    ratedRestaurant.add(template.load(recommendation, Recommendation.class));
                }
                return ratedRestaurant;
            }
        }

        private final Map<Node, AggregatedRecommendation> recommendedRestaurants=new HashMap<Node, AggregatedRecommendation>();

        public CalculateRatingPredicate() {
        }

        @Override
        public Evaluation evaluate(Path path) {
            final int distance = path.length();
            final Node friend = path.endNode();
            for (final Relationship recommendation : friend.getRelationships(DynamicRelationshipType.withName("recommends"))) {
                final Node restaurant = recommendation.getEndNode();
                if (!recommendedRestaurants.containsKey(restaurant)) {
                    recommendedRestaurants.put(restaurant,new AggregatedRecommendation(restaurant));
                }
                recommendedRestaurants.get(restaurant).add(recommendation,distance);
            }
            return Evaluation.INCLUDE_AND_CONTINUE;
        }

        public Collection<RatedRestaurant> getRecommendedRestaurants(final int n) {
            final List<AggregatedRecommendation> sorted = new ArrayList<AggregatedRecommendation>(recommendedRestaurants.values());
            Collections.sort(sorted);
            int index=0;
            final Collection<RatedRestaurant> result=new ArrayList<RatedRestaurant>(n);
            for (final AggregatedRecommendation aggregatedRecommendation : sorted) {
                if (index == n) return result;
                result.add(aggregatedRecommendation.toRatedRestaurant(this));
                index++;
            }
            return result;
        }

    }
}
