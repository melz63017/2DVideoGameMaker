package model.physics;

import api.IEntity;
import api.ILevel;
import api.IPhysicsEngine;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import model.component.movement.Position;
import model.component.movement.Velocity;
import model.component.physics.*;
import model.component.visual.ImagePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Implementation of the physics engine
 *
 * @author Tom Wu and Roxanne Baker
 */
public class PhysicsEngine implements IPhysicsEngine {

	private boolean gravityActive;
	private boolean collisionDetectionActive;
	private boolean frictionActive;

	public PhysicsEngine() {
		gravityActive = true;
		collisionDetectionActive = true;
		frictionActive = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(ILevel universe, double dt) {
		Collection<IEntity> entities = universe.getEntitiesWithComponents(Position.class, ImagePath.class);
		entities.stream().forEach(p -> {
			Position pos = p.getComponent(Position.class);
			ImageView imageView = p.getComponent(ImagePath.class).getImageView();
			imageView.setTranslateX(pos.getX());
			imageView.setTranslateY(pos.getY());
		});

		if (gravityActive) {
			applyGravity(universe, dt);
		}
		if (collisionDetectionActive) {
			applyCollisions(universe, true);
		}
		if (frictionActive) {
			applyFriction(universe, dt);
		}

		Collection<IEntity> dynamicEntities = universe.getEntitiesWithComponents(Position.class, Velocity.class);
		dynamicEntities.stream().forEach(p -> {
			Position pos = p.getComponent(Position.class);
			Velocity velocity = p.getComponent(Velocity.class);
			double dx = dt * velocity.getVX();
			double dy = dt * velocity.getVY();
			pos.add(dx, dy);
		});
	}

	@Override
	public boolean applyImpulse(IEntity body, Vector impulse) {
		if (body.hasComponents(Mass.class, Velocity.class)) {
			Velocity v = body.getComponent(Velocity.class);
			double m = body.getComponent(Mass.class).getMass();
			v.add(impulse.getXComponent() / m, impulse.getYComponent() / m);
			return true;
		} else {
			return false;
		}
	}

	public void applyGravity(ILevel universe, double secondsPassed) {
		Collection<IEntity> entitiesSubjectToGravity = universe.getEntitiesWithComponents(Gravity.class,
				Velocity.class);

		entitiesSubjectToGravity.stream().forEach(entity -> {
			// System.out.println(entity.getComponent(Collision.class).getCollidingIDs());
			// if
			// (!entity.getComponent(Collision.class).getCollidingIDs().contains(Collision.BOTTOM))
			// {
			// Position pos = entity.getComponent(Position.class);
			Gravity acceleration = entity.getComponent(Gravity.class);
			// pos.add(0, secondsPassed * secondsPassed * gravity);
			Velocity velocity = entity.getComponent(Velocity.class);
			velocity.add(acceleration.getGravityX(), acceleration.getGravityY());
			// }
		});
	}

	@Override
	public void applyCollisions(ILevel universe, boolean dynamicsOn) {
		List<IEntity> collidableEntities = new ArrayList<IEntity>(
				universe.getEntitiesWithComponents(Collision.class, ImagePath.class));// ,
																						// Mass.class));
		clearCollisionComponents(collidableEntities);

		for (int i = 0; i < collidableEntities.size(); i++) {
			for (int j = i + 1; j < collidableEntities.size(); j++) {
				addCollisionComponents(collidableEntities.get(i), collidableEntities.get(j));
			}
		}
	}

	private void addCollisionComponents(IEntity firstEntity, IEntity secondEntity) {
		List<Bounds> firstHitBoxes = getHitBoxesForEntity(firstEntity);
		List<Bounds> secondHitBoxes = getHitBoxesForEntity(secondEntity);

		for (Bounds firstHitBox : firstHitBoxes) {
			for (Bounds secondHitBox : secondHitBoxes) {
				if (firstHitBox.intersects(secondHitBox)) {
					addEntityIDs(firstEntity, secondEntity);
					addCollisionSide(firstEntity, secondEntity);
					changeVelocityAfterCollision(firstEntity, secondEntity);
					break; // TODO: what's the purpose of this?
				}
			}
		}
	}

	private void clearCollisionComponents(List<IEntity> collidableEntities) {
		for (IEntity entity : collidableEntities) {
			entity.getComponent(Collision.class).clearCollidingIDs();
			entity.getComponent(Collision.class)
					.setMask(entity.getComponent(ImagePath.class).getImageView().getBoundsInParent());
		}
	}

	public void changeVelocityAfterCollision(IEntity firstEntity, IEntity secondEntity) {
		Mass mass1 = firstEntity.getComponent(Mass.class);
		Mass mass2 = secondEntity.getComponent(Mass.class);
		if (mass1 == null || mass2 == null) {
			return; // TODO: other cases?
		}

		double restitution = getCollisionRestitution(firstEntity, secondEntity);
		double m1 = mass1.getMass();
		double m2 = mass2.getMass();
		Velocity velocity1 = getVelocityComponent(firstEntity);
		Velocity velocity2 = getVelocityComponent(secondEntity);

		// Reference: https://en.wikipedia.org/wiki/Coefficient_of_restitution
//		Vector u1 = new Vector(velocity1.getVX(), velocity1.getVY());
//		Vector u2 = new Vector(velocity2.getVX(), velocity2.getVY());
//		Vector numCommonTerm = u1.scalarMultiply(m1).add(u2.scalarMultiply(m2));
//		Vector num1Term = u2.add(u1.negate()).scalarMultiply(m2 * restitution);
//		Vector num2Term = u1.add(u2.negate()).scalarMultiply(m1 * restitution);
//		double denom = m1 + m2;
//		Vector v1 = numCommonTerm.add(num1Term).scalarMultiply(1.0 / denom);
//		Vector v2 = numCommonTerm.add(num2Term).scalarMultiply(1.0 / denom);
//
//		velocity1.setVXY(v1.getXComponent(), v1.getYComponent());
//		velocity2.setVXY(v2.getXComponent(), v2.getYComponent());
//		System.out.println(velocity1 + " -- " + velocity2);

		// TODO: move entity out of collision bounds depending on relative position/side

		 if (collisionIsHorizontal(firstEntity)) {
			 setVelocityComponent(m1, m2, velocity1, velocity2, restitution,
			 (Velocity v) -> v.getVX(),
			 (Velocity v, Double val) -> v.setVX(val));
		 }
		 if (collisionIsVertical(firstEntity)) {
			 setVelocityComponent(m1, m2, velocity1, velocity2, restitution,
			 (Velocity v) -> v.getVY(),
			 (Velocity v, Double val) -> v.setVY(val));
		 }
	}

	 private boolean collisionIsFromSide(IEntity entity, String side) {
	 	return entity.getComponent(Collision.class).getCollidingIDs().endsWith(side);
	 }

	 private boolean collisionIsHorizontal(IEntity entity) {
		 return collisionIsFromSide(entity, Collision.LEFT) ||
		 collisionIsFromSide(entity, Collision.RIGHT);
	 }

	 private boolean collisionIsVertical(IEntity entity) {
		 return collisionIsFromSide(entity, Collision.TOP) ||
		 collisionIsFromSide(entity, Collision.BOTTOM);
	 }

	/**
	 * 
	 * @param firstEntity
	 * @param secondEntity
	 * @return the coefficient of restitution to be used for a collision between
	 *         the two entities given
	 */
	private double getCollisionRestitution(IEntity firstEntity, IEntity secondEntity) {
		double firstRestitution = getEntityRestitution(firstEntity);
		double secondRestitution = getEntityRestitution(secondEntity);
		// if (firstRestitution < 0 && secondRestitution < 0) {
		// return 0.5;
		// }
		// else if (firstRestitution < 0 || secondRestitution < 0) {
		// return Math.max(firstRestitution, secondRestitution);
		// }
		// else {
		// return (firstRestitution + secondRestitution) / 2;
		// }
		return (firstRestitution + secondRestitution) / 2;
	}

	/**
	 * 
	 * @param entity
	 * @return the coefficient of restitution of the given entity, or 0 if none
	 *         exists
	 */
	private double getEntityRestitution(IEntity entity) {
		if (entity.getComponentList(RestitutionCoefficient.class).size() == 0) {
			return 0.0;
		} else {
			return entity.getComponent(RestitutionCoefficient.class).getRestitutionCoefficient();
		}
	}

	private Velocity getVelocityComponent(IEntity entity) {
		if (entity.getComponentList(Velocity.class).size() == 0) {
			return new Velocity(0, 0);
		} else {
			return entity.getComponent(Velocity.class);
		}
	}

	 private void setVelocityComponent(double mass1, double mass2, Velocity
	 velocity1, Velocity velocity2,
	 double restitution, Function<Velocity, Double> getCoordinate,
	 BiConsumer<Velocity, Double> setVelocity) {
		 double initialVelocity1 = getCoordinate.apply(velocity1);
		 double initialVelocity2 = getCoordinate.apply(velocity2);

		 double velocityBeforeRestitution = getVelocityBeforeRestitution(mass1,
		 mass2, initialVelocity1,
		 initialVelocity2);
		 double finalVelocity1 = velocityBeforeRestitution
		 + ((mass2 * restitution * (initialVelocity2 - initialVelocity1)) / (mass1
		 + mass2));
		 double finalVelocity2 = velocityBeforeRestitution
		 + ((mass1 * restitution * (initialVelocity1 - initialVelocity2)) / (mass1
		 + mass2));
		 setVelocity.accept(velocity1, finalVelocity1);
		 setVelocity.accept(velocity2, finalVelocity2);
	 }

	 private double getVelocityBeforeRestitution(double mass1, double mass2,
	 double velocity1, double velocity2) {
	 return ((mass1 * velocity1) + (mass2 * velocity2)) / (mass1 + mass2);
	 }

	private void addEntityIDs(IEntity firstEntity, IEntity secondEntity) {
		firstEntity.getComponent(Collision.class).addCollidingID(secondEntity.getID());
		secondEntity.getComponent(Collision.class).addCollidingID(firstEntity.getID());
	}

	private List<Bounds> getHitBoxesForEntity(IEntity entity) {
		List<Collision> collisionComponents = entity.getComponentList(Collision.class);
		List<Bounds> hitBoxes = new ArrayList<>();
		for (Collision hitBox : collisionComponents) {
			hitBoxes.add(hitBox.getMask());
		}
		return hitBoxes;
	}

	private void applyFriction(ILevel universe, double secondsPassed) {
		List<IEntity> frictionProneEntities = new ArrayList<IEntity>(
				universe.getEntitiesWithComponents(Friction.class, Gravity.class, Velocity.class));
		// TODO: should depend on collision, not gravity
		for (IEntity entity : frictionProneEntities) {
			double friction = entity.getComponent(Friction.class).getFriction();
			 double gravity = entity.getComponent(Gravity.class).getGravityY();
			 double acceleration = friction * gravity;
			 Velocity velocity = entity.getComponent(Velocity.class);
			 velocity.add(Math.max(-velocity.getVX(), -acceleration *
			 secondsPassed), 0);
		}
	}

	private void addCollisionSide(IEntity e1, IEntity e2) {
		Collision first = e1.getComponent(Collision.class);
		Collision second = e2.getComponent(Collision.class);

		if (first.getMask().getMaxX() > second.getMask().getMinX()) {
			first.addCollisionSide(Collision.RIGHT);
			second.addCollisionSide(Collision.LEFT);
		} else if (first.getMask().getMinX() < second.getMask().getMaxX()) {
			first.addCollisionSide(Collision.LEFT);
			second.addCollisionSide(Collision.RIGHT);
		}
		if (first.getMask().getMaxY() > second.getMask().getMinY()) {
			first.addCollisionSide(Collision.TOP);
			second.addCollisionSide(Collision.BOTTOM);
		} else if (first.getMask().getMinY() < second.getMask().getMaxY()) {
			first.addCollisionSide(Collision.BOTTOM);
			second.addCollisionSide(Collision.TOP);
		}
	}

	public void setGravityActive(boolean gravityActive) {
		this.gravityActive = gravityActive;
	}

	public void setCollisionDetectionActive(boolean collisionDetectionActive) {
		this.collisionDetectionActive = collisionDetectionActive;
	}

	public void setFrictionActive(boolean frictionActive) {
		this.frictionActive = frictionActive;
	}

}
