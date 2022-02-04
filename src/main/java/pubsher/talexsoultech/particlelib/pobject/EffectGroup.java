package pubsher.talexsoultech.particlelib.pobject;

import com.google.common.collect.Lists;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import pubsher.talexsoultech.particlelib.utils.matrix.Matrixs;

import java.util.Arrays;
import java.util.List;

/**
 * 代表一个特效组
 * <p>
 * 如果你要使用 EffectGroup#scale 这样的方法, 我不建议你将 2D 的特效和 3D 的特效放在一起
 * </p>
 *
 * @author Zoyn IceCold
 */
public class EffectGroup {

    /**
     * 特效表
     */
    private final List<ParticleObject> effectList;

    public EffectGroup() {

        effectList = Lists.newArrayList();
    }

    /**
     * 利用给定的特效列表构造出一个特效组
     *
     * @param effectList 特效列表
     */
    public EffectGroup(List<ParticleObject> effectList) {

        this.effectList = effectList;
    }

    /**
     * 往特效组添加一项特效
     *
     * @param particleObj 特效对象
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup addEffect(ParticleObject particleObj) {

        effectList.add(particleObj);
        return this;
    }

    /**
     * 往特效组添加一堆特效
     *
     * @param particleObj 一堆特效对象
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup addEffect(ParticleObject... particleObj) {

        effectList.addAll(Arrays.asList(particleObj));
        return this;
    }

    /**
     * 利用给定的下标, 将特效组里的第 index-1 个特效进行删除
     *
     * @param index 下标
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup removeEffect(int index) {

        effectList.remove(index);
        return this;
    }

    /**
     * 利用给定的数字, 设置每一个特效的循环 tick
     *
     * @param period 循环tick
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup setPeriod(long period) {

        effectList.forEach(effect -> effect.setPeriod(period));
        return this;
    }

    /**
     * 将特效组内的特效进行缩小或扩大
     *
     * @param value 缩小或扩大的倍率
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup scale(double value) {

        effectList.forEach(effect -> effect.addMatrix(Matrixs.scale(2, 2, value)));
        return this;
    }

    /**
     * 将特效组内的特效进行旋转
     *
     * @param angle 旋转角度
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup rotate(double angle) {

        effectList.forEach(effect -> effect.addMatrix(Matrixs.rotate2D(angle)));
        return this;
    }

    /**
     * 将特效组内的特效一次性展现出来
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup show() {

        effectList.forEach(ParticleObject::show);
        return this;
    }

    /**
     * 将特效组内的特效一直地展现出来
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup alwaysShow() {

        effectList.forEach(ParticleObject::alwaysShow);
        return this;
    }

    /**
     * 将特效组内的特效一直且异步地展现出来
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup alwaysShowAsync() {

        effectList.forEach(ParticleObject::alwaysShowAsync);
        return this;
    }

    /**
     * 将特效组内的特效同时播放出来
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup play() {

        for ( ParticleObject pObj : effectList ) {
            if ( pObj instanceof Playable ) {
                Playable playable = (Playable) pObj;
                playable.play();
            }
        }
        return this;
    }

    /**
     * 将特效组内的特效一直地同时播放出来
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup alwaysPlay() {

        for ( ParticleObject pObj : effectList ) {
            if ( pObj instanceof Playable ) {
                pObj.alwaysPlay();
            }
        }
        return this;
    }

    /**
     * 将特效组内的特效一直且异步地同时播放出来
     *
     * @return {@link EffectGroup}
     */
    public EffectGroup alwaysPlayAsync() {

        for ( ParticleObject pObj : effectList ) {
            if ( pObj instanceof Playable ) {
                pObj.alwaysPlayAsync();
            }
        }
        return this;
    }

    public EffectGroup attachEntity(Entity entity) {

        effectList.forEach(effect -> effect.attachEntity(entity));
//        effectList.forEach((effect) -> new BukkitRunnable() {
//            @Override
//            public void run() {
//                effect.setOrigin(entity.getLocation());
//            }
//        }.runTaskTimerAsynchronously(TalexSoulTech.getInstance(), 0, effect.getPeriod()));
        return this;
    }

    public EffectGroup setParticle(Particle particle) {

        effectList.forEach(effect -> effect.setParticle(particle));
        return this;
    }

    /**
     * 获取特效列表
     *
     * @return {@link List}
     */
    public List<ParticleObject> getEffectList() {

        return effectList;
    }

}
