package maninhouse.epicfight.capabilities.entity.player;

import java.util.UUID;

import maninhouse.epicfight.animation.LivingMotion;
import maninhouse.epicfight.animation.types.StaticAnimation;
import maninhouse.epicfight.capabilities.entity.DataKeys;
import maninhouse.epicfight.capabilities.entity.LivingData;
import maninhouse.epicfight.client.animation.AnimatorClient;
import maninhouse.epicfight.entity.event.EntityEventListener;
import maninhouse.epicfight.entity.event.EntityEventListener.Event;
import maninhouse.epicfight.entity.event.PlayerEvent;
import maninhouse.epicfight.gamedata.Animations;
import maninhouse.epicfight.gamedata.Models;
import maninhouse.epicfight.gamedata.Skills;
import maninhouse.epicfight.model.Model;
import maninhouse.epicfight.skill.SkillContainer;
import maninhouse.epicfight.skill.SkillSlot;
import maninhouse.epicfight.utils.game.IExtendedDamageSource;
import maninhouse.epicfight.utils.game.IExtendedDamageSource.DamageType;
import maninhouse.epicfight.utils.game.IExtendedDamageSource.StunType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;

public abstract class PlayerData<T extends PlayerEntity> extends LivingData<T> {
	private static final UUID ACTION_EVENT_UUID = UUID.fromString("e6beeac4-77d2-11eb-9439-0242ac130002");
	protected float yaw;
	protected EntityEventListener eventListeners;
	protected int tickSinceLastAction;
	public SkillContainer[] skills;
	
	public PlayerData() {
		SkillSlot[] slots = SkillSlot.values();
		this.skills = new SkillContainer[SkillSlot.values().length];
		for(SkillSlot slot : slots)
			this.skills[slot.getIndex()] = new SkillContainer(this);
	}
	
	@Override
	public boolean onEntityJoinWorld(T entityIn) {
		if(super.onEntityJoinWorld(entityIn)) {
			this.eventListeners = new EntityEventListener(this);
			this.skills[SkillSlot.DODGE.getIndex()].setSkill(Skills.ROLL);
			this.orgEntity.getDataManager().register(DataKeys.STUN_ARMOR, Float.valueOf(0.0F));
			this.tickSinceLastAction = 40;
			this.eventListeners.addEventListener(Event.ON_ACTION_SERVER_EVENT, PlayerEvent.makeEvent(ACTION_EVENT_UUID, (player)->{
				player.tickSinceLastAction = 0;
				return false;
			}));
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void initAnimator(AnimatorClient animatorClient) {
		animatorClient.mixLayer.setJointMask("Root", "Torso");
		animatorClient.addLivingAnimation(LivingMotion.IDLE, Animations.BIPED_IDLE);
		animatorClient.addLivingAnimation(LivingMotion.WALKING, Animations.BIPED_WALK);
		animatorClient.addLivingAnimation(LivingMotion.RUNNING, Animations.BIPED_RUN);
		animatorClient.addLivingAnimation(LivingMotion.SNEAKING, Animations.BIPED_SNEAK);
		animatorClient.addLivingAnimation(LivingMotion.SWIMMING, Animations.BIPED_SWIM);
		animatorClient.addLivingAnimation(LivingMotion.FLOATING, Animations.BIPED_FLOAT);
		animatorClient.addLivingAnimation(LivingMotion.KNEELING, Animations.BIPED_KNEEL);
		animatorClient.addLivingAnimation(LivingMotion.FALL, Animations.BIPED_FALL);
		animatorClient.addLivingAnimation(LivingMotion.MOUNT, Animations.BIPED_MOUNT);
		animatorClient.addLivingAnimation(LivingMotion.FLYING, Animations.BIPED_FLYING);
		animatorClient.addLivingAnimation(LivingMotion.DEATH, Animations.BIPED_DEATH);
		animatorClient.addLivingAnimation(LivingMotion.JUMPING, Animations.BIPED_JUMP);
		animatorClient.addLivingMixAnimation(LivingMotion.BLOCKING, Animations.BIPED_BLOCK);
		animatorClient.addLivingMixAnimation(LivingMotion.AIMING, Animations.BIPED_BOW_AIM);
		animatorClient.addLivingMixAnimation(LivingMotion.RELOADING, Animations.BIPED_CROSSBOW_RELOAD);
		animatorClient.addLivingMixAnimation(LivingMotion.SHOTING, Animations.BIPED_BOW_REBOUND);
		animatorClient.setCurrentLivingMotionsToDefault();
	}
	
	public void changeYaw(float amount) {
		this.yaw = amount;
	}
	
	@Override
	public void updateOnServer() {
		super.updateOnServer();
		this.tickSinceLastAction++;
		
		float stunArmor = this.getStunArmor();
		float maxStunArmor = this.getMaxStunArmor();
		
		if(stunArmor < maxStunArmor && this.tickSinceLastAction > 60) {
			float stunArmorFactor = 1.0F + (stunArmor / maxStunArmor);
			float healthFactor = this.orgEntity.getHealth() / this.orgEntity.getMaxHealth();
			this.setStunArmor(stunArmor + maxStunArmor * 0.01F * healthFactor * stunArmorFactor);
		}
	}
	
	@Override
	public void update() {
		if(this.orgEntity.getRidingEntity() == null) {
			for(SkillContainer container : this.skills) {
				if(container != null)
					container.update();
			}
		}
		
		super.update();
	}
	
	public SkillContainer getSkill(SkillSlot slot) {
		return this.skills[slot.getIndex()];
	}
	
	public SkillContainer getSkill(int slotIndex) {
		return this.skills[slotIndex];
	}
	
	public float getWeightPaneltyDivider() {
		return (float) (40.0F / this.getWeight());
	}
	
	public float getAttackSpeed() {
		return (float) orgEntity.getAttributeValue(Attributes.ATTACK_SPEED);
	}
	
	public EntityEventListener getEventListener() {
		return this.eventListeners;
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource damageSource, float amount) {
		if(super.attackEntityFrom(damageSource, amount)) {
			this.tickSinceLastAction = 0;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public IExtendedDamageSource getDamageSource(StunType stunType, DamageType damageType, int id) {
		return IExtendedDamageSource.causePlayerDamage(orgEntity, stunType, damageType, id);
	}
	
	public void discard() {
		super.aboutToDeath();
	}
	
	@Override
	public StaticAnimation getHitAnimation(StunType stunType) {
		if(orgEntity.getRidingEntity() != null) {
			return Animations.BIPED_HIT_ON_MOUNT;
		} else {
			switch(stunType)
			{
			case LONG:
				return Animations.BIPED_HIT_LONG;
			case SHORT:
				return Animations.BIPED_HIT_SHORT;
			case HOLD:
				return Animations.BIPED_HIT_SHORT;
			default:
				return null;
			}
		}
	}
	
	@Override
	public <M extends Model> M getEntityModel(Models<M> modelDB) {
		return modelDB.ENTITY_BIPED;
	}
}