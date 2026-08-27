import { CAMPAIGN_WAVES } from './progression.js';

const STATUS = Object.freeze({
  implemented: 'implemented',
  planned: 'planned',
});

const STAGE_LABELS = Object.freeze({
  1: '第一阶段·生存奠基',
  2: '第二阶段·生产扩展',
  3: '第三阶段·系统自动化',
  4: '第四阶段·远界工程',
});

const TIERS = Object.freeze({
  1: Object.freeze(['I', 'II', 'III']),
  2: Object.freeze(['IV', 'V', 'VI']),
  3: Object.freeze(['VII', 'VIII', 'IX']),
  4: Object.freeze(['X', 'XI', 'XII']),
});
const TIER_RANK = Object.freeze({
  I: 1,
  II: 2,
  III: 3,
  IV: 4,
  V: 5,
  VI: 6,
  VII: 7,
  VIII: 8,
  IX: 9,
  X: 10,
  XI: 11,
  XII: 12,
});

const PROGRESSION_INDEX = (() => {
  const disciplineById = new Map();
  const anchorByFamilyKey = new Map();
  const storyByItemId = new Map();
  for (const wave of CAMPAIGN_WAVES) {
    for (const arc of wave.disciplineArcs) {
      disciplineById.set(arc.id, { wave, arc });
    }
    for (const anchor of wave.anchors) {
      anchorByFamilyKey.set(anchor.familyKey, { wave, anchor });
      for (const story of anchor.stories) {
        storyByItemId.set(story.itemId, {
          waveId: wave.id,
          familyKey: anchor.familyKey,
          order: story.order,
          text: story.text,
          anchorReason: anchor.reason,
        });
      }
    }
  }
  return Object.freeze({ disciplineById, anchorByFamilyKey, storyByItemId });
})();


const I = (slug, name, tier, type, purpose, recipeHint, status = STATUS.planned) => Object.freeze({
  slug,
  name,
  tier,
  type,
  purpose,
  recipeHint,
  status,
});

const E = (id, name, concept, entries) => Object.freeze({
  id,
  name,
  concept,
  entries: Object.freeze(entries),
});

const P = (id, name, concept, stem, kind, operation, ingredients) => Object.freeze({
  id,
  name,
  concept,
  stem,
  kind,
  operation,
  ingredients,
});

const D = ({ id, stage, name, tagline, overview, learningGoals, families, status = STATUS.planned }) => Object.freeze({
  id,
  stage,
  name,
  tagline,
  overview,
  learningGoals: Object.freeze(learningGoals),
  families: Object.freeze(families),
  status,
});

const FAMILY_FORMS = Object.freeze({
  research: Object.freeze([
    Object.freeze({ slug: 'probe', suffix: '探针', type: '研究工具', purpose: (operation) => `手持右键目标方块后，${operation}。` }),
    Object.freeze({ slug: 'analyzer', suffix: '解析模组', type: '功能部件', purpose: (operation) => `装入同系列解析台后，解析台每 20 tick ${operation}。` }),
    Object.freeze({ slug: 'station', suffix: '观测站', type: '研究设施', purpose: (operation) => `放置并接入能量后，观测站每 20 tick ${operation}。` }),
  ]),
  resource: Object.freeze([
    Object.freeze({ slug: 'fragment', suffix: '碎片', type: '材料', purpose: (operation) => `投入资源加工槽后，${operation}。` }),
    Object.freeze({ slug: 'alloy', suffix: '合金', type: '材料', purpose: (operation) => `作为高级合成材料时，${operation}。` }),
    Object.freeze({ slug: 'block', suffix: '储块', type: '结构方块', purpose: (operation) => `放置在工作区后，${operation}。` }),
  ]),
  processing: Object.freeze([
    Object.freeze({ slug: 'reagent', suffix: '试剂', type: '消耗品', purpose: (operation) => `加入处理槽后，${operation}。` }),
    Object.freeze({ slug: 'core', suffix: '反应芯', type: '加工部件', purpose: (operation) => `装入反应设备后，设备每次作业 ${operation}。` }),
    Object.freeze({ slug: 'vat', suffix: '反应釜', type: '加工设施', purpose: (operation) => `放置并加热后，每 20 tick ${operation}。` }),
  ]),
  plant: Object.freeze([
    Object.freeze({ slug: 'seed', suffix: '种子', type: '种植材料', purpose: (operation) => `播种在耕地上后，${operation}。` }),
    Object.freeze({ slug: 'culture', suffix: '培养剂', type: '农用消耗品', purpose: (operation) => `对成熟植株使用后，${operation}。` }),
    Object.freeze({ slug: 'greenhouse', suffix: '温室', type: '农业设施', purpose: (operation) => `放置并供水后，每 20 tick ${operation}。` }),
  ]),
  defense: Object.freeze([
    Object.freeze({ slug: 'plate', suffix: '护片', type: '防具部件', purpose: (operation) => `装配到护甲后，${operation}。` }),
    Object.freeze({ slug: 'armor', suffix: '护甲', type: '防具', purpose: (operation) => `穿戴后，${operation}。` }),
    Object.freeze({ slug: 'bastion', suffix: '壁垒', type: '防御设施', purpose: (operation) => `放置并激活后，每 20 tick ${operation}。` }),
  ]),
  machine: Object.freeze([
    Object.freeze({ slug: 'part', suffix: '零件', type: '机械部件', purpose: (operation) => `装入作业机后，${operation}。` }),
    Object.freeze({ slug: 'drive', suffix: '机芯', type: '机械核心', purpose: (operation) => `为连接的机器提供工作逻辑，使其 ${operation}。` }),
    Object.freeze({ slug: 'workstation', suffix: '作业台', type: '机器', purpose: (operation) => `放置并供能后，每次作业 ${operation}。` }),
  ]),
  energy: Object.freeze([
    Object.freeze({ slug: 'coil', suffix: '线圈', type: '电气部件', purpose: (operation) => `接入电网后，${operation}。` }),
    Object.freeze({ slug: 'cell', suffix: '电池', type: '储能设备', purpose: (operation) => `连接电网后，${operation}。` }),
    Object.freeze({ slug: 'unit', suffix: '机组', type: '能源设施', purpose: (operation) => `放置并接入电网后，每 20 tick ${operation}。` }),
  ]),
  magic: Object.freeze([
    Object.freeze({ slug: 'rune', suffix: '符文', type: '法术材料', purpose: (operation) => `嵌入法杖后，施放时 ${operation}。` }),
    Object.freeze({ slug: 'wand', suffix: '法器', type: '法术工具', purpose: (operation) => `手持右键施放后，${operation}。` }),
    Object.freeze({ slug: 'array', suffix: '法阵', type: '魔法设施', purpose: (operation) => `放置并注入魔能后，每 20 tick ${operation}。` }),
  ]),
  space: Object.freeze([
    Object.freeze({ slug: 'shard', suffix: '碎片', type: '空间材料', purpose: (operation) => `在传送框架附近使用后，${operation}。` }),
    Object.freeze({ slug: 'anchor', suffix: '锚', type: '空间装置', purpose: (operation) => `放置并绑定坐标后，${operation}。` }),
    Object.freeze({ slug: 'gate', suffix: '门', type: '传送设施', purpose: (operation) => `激活后，每 20 tick ${operation}。` }),
  ]),
  gravity: Object.freeze([
    Object.freeze({ slug: 'mass', suffix: '砝码', type: '引力材料', purpose: (operation) => `放置在引力台上后，${operation}。` }),
    Object.freeze({ slug: 'gauntlet', suffix: '手套', type: '引力工具', purpose: (operation) => `手持右键后，${operation}。` }),
    Object.freeze({ slug: 'field', suffix: '场发生器', type: '引力设施', purpose: (operation) => `放置并供能后，每 20 tick ${operation}。` }),
  ]),
  logistics: Object.freeze([
    Object.freeze({ slug: 'tag', suffix: '标签', type: '物流部件', purpose: (operation) => `贴附到容器后，${operation}。` }),
    Object.freeze({ slug: 'sorter', suffix: '分拣器', type: '物流设备', purpose: (operation) => `接入库存后，每 20 tick ${operation}。` }),
    Object.freeze({ slug: 'relay', suffix: '中继站', type: '物流设施', purpose: (operation) => `放置并配置频道后，${operation}。` }),
  ]),
  construction: Object.freeze([
    Object.freeze({ slug: 'brick', suffix: '砖', type: '建筑材料', purpose: (operation) => `用于建筑时，${operation}。` }),
    Object.freeze({ slug: 'frame', suffix: '框架', type: '结构部件', purpose: (operation) => `搭建为完整结构后，${operation}。` }),
    Object.freeze({ slug: 'workshop', suffix: '工坊', type: '建筑设施', purpose: (operation) => `放置并启用后，每 20 tick ${operation}。` }),
  ]),
  fluid: Object.freeze([
    Object.freeze({ slug: 'filter', suffix: '滤芯', type: '流体部件', purpose: (operation) => `装入储罐或管道后，${operation}。` }),
    Object.freeze({ slug: 'pump', suffix: '泵', type: '流体设备', purpose: (operation) => `接入液体网络后，每 20 tick ${operation}。` }),
    Object.freeze({ slug: 'network', suffix: '管网', type: '流体设施', purpose: (operation) => `完成管道连接后，每 20 tick ${operation}。` }),
  ]),
  commerce: Object.freeze([
    Object.freeze({ slug: 'token', suffix: '凭证', type: '交易物品', purpose: (operation) => `在交易终端使用后，${operation}。` }),
    Object.freeze({ slug: 'contract', suffix: '合约板', type: '管理设备', purpose: (operation) => `写入订单后，${operation}。` }),
    Object.freeze({ slug: 'exchange', suffix: '交易所', type: '经济设施', purpose: (operation) => `放置并授权后，每 20 tick ${operation}。` }),
  ]),
  quantum: Object.freeze([
    Object.freeze({ slug: 'bit', suffix: '量子位', type: '量子部件', purpose: (operation) => `写入量子缓存后，${operation}。` }),
    Object.freeze({ slug: 'core', suffix: '量子核', type: '量子核心', purpose: (operation) => `装入量子设备后，${operation}。` }),
    Object.freeze({ slug: 'gate', suffix: '量子门', type: '量子设施', purpose: (operation) => `放置并稳定供能后，每 20 tick ${operation}。` }),
  ]),
});
const EXPLICIT_FAMILY_KINDS = Object.freeze({
  'basic.wood-compression': 'resource',
  'basic.log-compression': 'resource',
  'basic.stick-compression': 'resource',
  'basic.breaking-starter': 'machine',
  'basic.breaking-advanced': 'machine',
  'basic.sieving': 'resource',
  'materials.fire-materials': 'resource',
  'materials.reinforced-thread': 'resource',
  'botany.growth-aid': 'plant',
  'botany.seed-cultivation': 'plant',
  'botany.mushroom-cultivation': 'plant',
  'botany.resin-harvesting': 'resource',
  'defense.heat-armor': 'defense',
  'defense.mobility-armor': 'defense',
  'technology.electric-components': 'energy',
  'technology.thermal-generation': 'energy',
  'technology.energy-storage': 'energy',
  'technology.fluid-tank': 'fluid',
  'technology.registered-machines-a': 'machine',
  'technology.registered-machines-b': 'machine',
  'magic.wand-core': 'magic',
  'magic.display-ritual': 'magic',
  'space.space-dust': 'space',
});

function familyKindFor(familyKey, family) {
  const familyKind = family.kind ?? EXPLICIT_FAMILY_KINDS[familyKey];
  if (!familyKind || !Object.prototype.hasOwnProperty.call(FAMILY_FORMS, familyKind)) {
    throw new Error(`缺少 ${familyKey} 的 family kind`);
  }
  return familyKind;
}


function plannedEntries(family, stage) {
  const forms = FAMILY_FORMS[family.kind];
  if (!forms) {
    throw new Error(`未知物品 family 类型：${family.kind}`);
  }
  const tiers = TIERS[stage];
  return forms.map((form, index) => I(
    form.slug,
    `${family.stem}${form.suffix}`,
    tiers[index],
    form.type,
    form.purpose(family.operation),
    index === 0
      ? `${family.ingredients}在基础工作台以 3×3 格局制作`
      : `${family.ingredients}与${family.stem}${forms[index - 1].suffix}在高级工作台组装`,
  ));
}

function assembleDiscipline(blueprint) {
  const disciplineProgression = PROGRESSION_INDEX.disciplineById.get(blueprint.id);
  if (!disciplineProgression) {
    throw new Error(`缺少 ${blueprint.id} 的主线波次与职责`);
  }
  const { wave, arc } = disciplineProgression;
  const families = blueprint.families.map(({ id, name, concept, kind, stem, operation, ingredients }) => {
    const familyKey = `${blueprint.id}.${id}`;
    const anchor = PROGRESSION_INDEX.anchorByFamilyKey.get(familyKey);
    return Object.freeze({
      id,
      name,
      concept,
      key: familyKey,
      familyKind: familyKindFor(familyKey, { kind }),
      stem: stem ?? name,
      operation: operation ?? null,
      ingredients: ingredients ?? null,
      isNarrativeAnchor: Boolean(anchor),
      anchorReason: anchor?.anchor.reason ?? null,
    });
  });
  const familyByKey = new Map(families.map((family) => [family.key, family]));
  const items = blueprint.families.flatMap((family) => {
    const familyKey = `${blueprint.id}.${family.id}`;
    const familyMeta = familyByKey.get(familyKey);
    const familyKind = familyMeta.familyKind;
    const familyForms = FAMILY_FORMS[familyKind];
    const entries = family.entries ?? plannedEntries(family, blueprint.stage);
    const familyItems = entries.map((entry, declarationIndex) => {
      const id = `${blueprint.id}.${family.id}.${entry.slug}`;
      const story = PROGRESSION_INDEX.storyByItemId.get(id);
      return {
        id,
        name: entry.name,
        tier: entry.tier,
        type: entry.type,
        purpose: entry.purpose,
        status: entry.status,
        disciplineId: blueprint.id,
        family: family.name,
        familyId: family.id,
        familyKey,
        familyKind,
        form: null,
        waveId: wave.id,
        recipeHint: entry.recipeHint,
        isNarrativeAnchor: Boolean(story),
        story: story
          ? Object.freeze({ order: story.order, text: story.text, anchorReason: story.anchorReason })
          : null,
        previousItemId: null,
        nextItemId: null,
        declarationIndex,
      };
    });
    const ordered = [...familyItems].sort((left, right) => (
      TIER_RANK[left.tier] - TIER_RANK[right.tier]
      || left.declarationIndex - right.declarationIndex
    ));
    ordered.forEach((item, index) => {
      item.form = familyForms[index].slug;
      item.previousItemId = ordered[index - 1]?.id ?? null;
      item.nextItemId = ordered[index + 1]?.id ?? null;
    });
    return familyItems.map(({ declarationIndex, ...item }) => Object.freeze(item));
  });
  return Object.freeze({
    id: blueprint.id,
    stage: blueprint.stage,
    stageLabel: STAGE_LABELS[blueprint.stage],
    waveId: wave.id,
    progression: Object.freeze({
      role: arc.role,
      whyNow: arc.whyNow,
      input: arc.input,
      output: arc.output,
      recovery: arc.recovery,
    }),
    name: blueprint.name,
    tagline: blueprint.tagline,
    overview: blueprint.overview,
    learningGoals: Object.freeze([...blueprint.learningGoals]),
    families: Object.freeze(families),
    items: Object.freeze(items),
    status: blueprint.status,
  });
}

const BLUEPRINTS = Object.freeze([
  D({
    id: 'basic', stage: 1, name: '基础学', status: STATUS.implemented,
    tagline: '把生存资源整理成可重复执行的第一条工艺链。',
    overview: '基础学覆盖压缩、破碎、筛分和现场作业，负责将原版资源转化为后续学科稳定使用的原料。',
    learningGoals: ['区分压缩、破碎与筛分的输入输出关系。', '选择匹配方块类型的破碎锤。', '用基础材料完成首个工艺工作区。'],
    families: [
      E('wood-compression', '木板压缩', '把木板压成三个密度等级以节省背包空间。', [
        I('plank-9', '压缩木板 ×9', 'I', '资源块', '将 9 块橡木木板压入一个物品格，潜行丢弃后还原为 9 块木板。', '工作台 3×3 放置九块橡木木板', STATUS.implemented),
        I('plank-81', '压缩木板 ×81', 'II', '资源块', '将 9 个压缩木板 ×9 合成一个 ×81 压缩板，用于高密度运输。', '工作台 3×3 放置九个压缩木板 ×9', STATUS.implemented),
        I('plank-729', '压缩木板 ×729', 'III', '资源块', '将 9 个压缩木板 ×81 合成一个 ×729 压缩板，提供大型配方所需的木材密度。', '工作台 3×3 放置九个压缩木板 ×81', STATUS.implemented),
      ]),
      E('log-compression', '原木压缩', '将原木压成便于储存与还原的高密度材料。', [
        I('log', '压缩原木', 'I', '资源块', '将原木压缩为单格材料，潜行丢弃后可还原为原木。', '工作台 3×3 放置同种原木', STATUS.implemented),
        I('log-super', '超级压缩原木', 'II', '资源块', '将九个压缩原木再次压合，作为中阶木质配方输入。', '工作台 3×3 放置九个压缩原木', STATUS.implemented),
        I('log-top', '顶级压缩原木', 'III', '资源块', '将九个超级压缩原木压成顶级原木，为大型结构保留 729 份木材。', '工作台 3×3 放置九个超级压缩原木', STATUS.implemented),
      ]),
      E('stick-compression', '木棒压缩', '建立木棒的多级压缩链，服务于工具与机器结构。', [
        I('stick', '压缩木棒', 'I', '材料', '将普通木棒压成高密度柄材，供破碎锤与设备配方消耗。', '工作台 3×3 放置九根木棒', STATUS.implemented),
        I('stick-1', '一级压缩木棒', 'II', '材料', '将九根压缩木棒压为一级柄材，降低长途搬运的格位占用。', '工作台 3×3 放置九根压缩木棒', STATUS.implemented),
        I('stick-2', '二级压缩木棒', 'III', '材料', '将九根一级压缩木棒压为二级柄材，提供高韧性工具输入。', '工作台 3×3 放置九根一级压缩木棒', STATUS.implemented),
      ]),
      E('breaking-starter', '基础破碎', '用石锤与铁锤把指定方块转化为随机资源。', [
        I('stick-3', '三级压缩木棒', 'III', '材料', '将九根二级压缩木棒压为三级柄材，作为最高级现有压缩木棒。', '工作台 3×3 放置九根二级压缩木棒', STATUS.implemented),
        I('stone-hammer', '破碎锤(石)', 'I', '工具', '手持敲击允许的基础方块，按破碎配方产出对应资源。', '在向导书的破碎锤配方页制作', STATUS.implemented),
        I('iron-hammer', '破碎锤(铁)', 'II', '工具', '手持敲击圆石或砂砾时提高煤炭、铁矿、青金石与红石的产出机会。', '在向导书的破碎锤配方页制作', STATUS.implemented),
      ]),
      E('breaking-advanced', '进阶破碎', '扩展破碎锤的材质与可执行配方链。', [
        I('gold-hammer', '破碎锤(金)', 'III', '工具', '手持敲击末地石时执行高阶破碎产出判定。', '通过冶炼锅炉处理已注册破碎锤配方', STATUS.implemented),
        I('gold-axe-hammer', '破碎锤斧(金)', 'III', '工具', '手持敲击圆石、下界岩与木质方块时执行斧类破碎产出判定。', '在向导书的破碎锤配方页制作', STATUS.implemented),
        I('recipe-chain', '破碎锤配方链', 'III', '工艺配方', '按圆石、砂砾与末地石的破碎配方持续提供煤炭、红石、铁矿、金矿与青金石。', '使用已注册破碎锤敲击配方指定方块', STATUS.implemented),
      ]),
      E('sieving', '筛分工具', '将方块筛分为资源并记录筛网耐久。', [
        I('normal-mesh', '普通筛网', 'I', '工具', '安装到筛子后可执行 20 次基础筛分并消耗筛网耐久。', '在基础工作台按普通筛网配方制作', STATUS.implemented),
        I('advanced-mesh', '高级筛网', 'II', '工具', '安装到筛子后可执行 120 次筛分，减少频繁更换筛网的中断。', '在高级工作台按高级筛网配方制作', STATUS.implemented),
        I('mesh-repair-clamp', '筛网修复夹', 'III', '维护工具', '对耗损筛网使用后恢复 25 点耐久，避免筛分作业因耐久归零停机。', '铁锭、线与黏液球在高级工作台组装'),
      ]),
      P('crafting-record', '配方档案', '把已学工艺写入可检索的配方记录。', '配方档案', 'research', '记录当前工作台中九个格位的物品排列并保存 30 分钟', '纸、墨囊与红石粉'),
      P('tool-service', '工具保养', '用标准维护件延长现场工具的连续作业时间。', '工具保养', 'machine', '在耐久低于 20% 时自动停止作业并提示维护位置', '铁锭、皮革与煤炭'),
      P('field-storage', '现场收纳', '在采集点建立不依赖玩家背包的物资缓存。', '现场收纳', 'logistics', '把标记物品送入半径 8 格内的指定箱子并保留原有分类标签', '箱子、漏斗与红石粉'),
      P('worksite-safety', '工地安全', '用可见提示划分工具作业区域。', '工地安全', 'construction', '在半径 6 格内投射黄色边界粒子并阻止未授权玩家启动机器', '黄染料、玻璃与铁锭'),
    ],
  }),
  D({
    id: 'materials', stage: 1, name: '材料学', status: STATUS.implemented,
    tagline: '让材料的耐热、强度与导电性成为可设计的属性。',
    overview: '材料学组织火焰材料、纤维与基础加工介质，为装备、能源与机器提供稳定的构件来源。',
    learningGoals: ['识别耐热材料与普通材料的使用边界。', '选择材料链中的锭、块与纤维输入。', '用材料属性解释装备与机器的用途。'],
    families: [
      E('fire-materials', '火焰材料', '现有火焰锭与火焰块用于耐热配方。', [
        I('fire-ingot', '火焰锭', 'II', '材料', '作为耐热配方原料，提供火焰装备与树脂加工所需的灼热材料。', '在冶炼锅炉处理已注册火焰材料配方', STATUS.implemented),
        I('fire-block', '火焰块', 'III', '资源块', '由火焰锭压合而成，用于需要高热密度的装备与机器配方。', '工作台 3×3 放置火焰锭', STATUS.implemented),
        I('fire-rod', '火焰棒', 'I', '材料', '将木棍在冶炼锅炉中持续加热后得到，作为火焰系配方与魔法法杖的基础热源。', '按已注册 fire_stick 冶炼锅配方加工木棍', STATUS.implemented),
      ]),
      E('reinforced-thread', '强力丝线', '现有强力丝线为装备与网具提供韧性。', [
        I('super-string', '强力丝线', 'I', '材料', '作为高韧性线材输入，连接筛网、护具与可动部件。', '在已注册材料配方中获得', STATUS.implemented),
        I('thread-spool', '强力丝线卷', 'II', '材料', '将 16 根强力丝线卷为单个线卷，减少纤维运输占用。', '工作台 3×3 放置强力丝线与纸', ),
        I('thread-mesh', '韧性编织网', 'III', '防具部件', '装配到胸甲后把一次坠落伤害降低 10%。', '强力丝线卷、铁锭与皮革在高级工作台组装'),
      ]),
      P('stone-bond', '石材粘结', '让建筑石材形成抗震的稳定结构。', '石材粘结', 'construction', '使相邻 6 块石材获得 10 秒的爆炸抗性标记', '圆石、砂砾与黏液球'),
      P('metal-grain', '金属晶粒', '观察金属晶粒方向对构件强度的影响。', '金属晶粒', 'resource', '使相邻机器零件的耐久消耗降低 5%', '铁锭、金锭与石英'),
      P('ceramic-shell', '陶瓷外壳', '为高热作业件提供绝缘与隔热边界。', '陶瓷外壳', 'processing', '将处理槽内液体的温度损失降低 1 点每 20 tick', '黏土、沙子与火焰粉'),
      P('glass-optics', '玻璃光学', '用透明材料制造可读的观测窗口。', '玻璃光学', 'research', '显示前方 16 格内液体储罐的液量与容量', '玻璃、红石粉与铜锭'),
      P('rubber-compound', '弹性复合物', '将树脂与纤维制成抗振连接材料。', '弹性复合物', 'processing', '使连接的动力轴在急停时不掉落物品', '树脂、强力丝线与煤炭'),
      P('conductive-alloy', '导电合金', '为电网部件设定可承受的稳定电流。', '导电合金', 'energy', '将导线经过的单段损耗固定为 0.2%', '铜锭、铁锭与红石粉'),
      P('composite-panel', '复合板材', '把不同材料层叠为用途明确的功能板。', '复合板材', 'construction', '使安装在其上的机器受到的雨雪影响延迟 60 秒', '木板、铁锭与树脂'),
      P('scrap-recovery', '废料回收', '从损坏构件中回收可再次使用的材料份额。', '废料回收', 'machine', '每次拆解损坏零件时返还 1 份对应基础材料', '剪刀、铁锭与木板'),
    ],
  }),
  D({
    id: 'botany', stage: 1, name: '植物学', status: STATUS.implemented,
    tagline: '把作物、生长介质与树脂采集整理为稳定农艺。',
    overview: '植物学连接种子、蘑菇、超级骨粉与树脂，使可再生资源能够服务于材料和生产链。',
    learningGoals: ['根据作物选择耕地、菌床与树叶采集方式。', '理解骨粉催熟与种植循环的差异。', '用树脂和纤维构建可再生材料来源。'],
    families: [
      E('growth-aid', '生长助剂', '现有超级骨粉直接催熟普通作物。', [
        I('super-bone-meal', '超级骨粉', 'II', '农用消耗品', '对普通作物使用后使其直接成熟，魔法作物不会响应此效果。', '在已注册食物与农艺配方中获得', STATUS.implemented),
        I('root-tonic', '根系营养液', 'II', '农用消耗品', '对耕地使用后让下一株普通作物的生长 tick 缩短 25%。', '骨粉、甜菜根与水瓶在高级工作台组装'),
        I('harvest-charm', '收获护符', 'III', '农用工具', '右键成熟普通作物后自动收集掉落物并保留作物根茎。', '金锭、超级骨粉与强力丝线在高级工作台组装'),
      ]),
      E('seed-cultivation', '种子培育', '现有筛子配方能够取得多种作物种子。', [
        I('pumpkin-seed', '南瓜种子', 'I', '种植材料', '在耕地种植后长成南瓜藤，并可通过筛子工艺取得。', '将泥土投入已注册筛子配方', STATUS.implemented),
        I('melon-seed', '西瓜种子', 'I', '种植材料', '在耕地种植后长成西瓜藤，并可通过筛子工艺取得。', '将泥土投入已注册筛子配方', STATUS.implemented),
        I('beetroot-seed', '甜菜根种子', 'I', '种植材料', '在耕地种植后长成甜菜根，并可通过筛子工艺取得。', '将泥土投入已注册筛子配方', STATUS.implemented),
      ]),
      E('mushroom-cultivation', '蘑菇培育', '现有筛子配方能够在灵魂沙条件下获得蘑菇。', [
        I('brown-mushroom', '棕色蘑菇', 'I', '种植材料', '在阴暗菌床种植后扩散，并可通过灵魂沙筛分取得。', '将灵魂沙投入已注册筛子配方', STATUS.implemented),
        I('red-mushroom', '红色蘑菇', 'I', '种植材料', '在阴暗菌床种植后扩散，并可通过灵魂沙筛分取得。', '将灵魂沙投入已注册筛子配方', STATUS.implemented),
        I('spore-lantern', '孢子灯笼', 'III', '农业设施', '放置后在半径 5 格内维持蘑菇可生长的低光照标记。', '蘑菇、玻璃与火把在高级工作台组装'),
      ]),
      E('resin-harvesting', '树脂采集', '现有树脂提取器、粘性树脂与树脂构成树叶采集链。', [
        I('resin-extractor', '树脂提取器', 'I', '工具', '手持破坏树叶后有机会直接取得树脂材料。', '在基础工作台按树脂提取器配方制作', STATUS.implemented),
        I('sticky-resin', '粘性树脂', 'I', '材料', '投入水中丢弃后可转化为可用于工艺的树脂。', '通过树脂提取器采集树叶获得', STATUS.implemented),
        I('resin', '树脂', 'II', '材料', '作为绝缘、弹性与装备配方的现有基础树脂材料。', '将粘性树脂投入水中处理', STATUS.implemented),
      ]),
      P('soil-profile', '土壤剖面', '测量土壤含水量与肥力变化。', '土壤剖面', 'plant', '使半径 4 格耕地的干湿状态显示为绿色或棕色粒子', '泥土、玻璃瓶与木棍'),
      P('pollination', '授粉管理', '让授粉对象在作物间形成明确的增产路径。', '授粉管理', 'plant', '使半径 6 格内两株同类作物的成熟 tick 缩短 10%', '蜂蜜瓶、花与纸'),
      P('composting', '堆肥循环', '把可腐化掉落物转为可计量的肥力。', '堆肥循环', 'processing', '每消耗 16 个植物掉落物生成 1 份可用于耕地的肥力', '堆肥桶、骨粉与树叶'),
      P('grafting', '嫁接培育', '把一株作物的生长特性写入另一株砧木。', '嫁接培育', 'plant', '使目标作物在一次成熟后额外掉落 1 个种子', '剪刀、木棍与黏液球'),
      P('pest-guard', '虫害防治', '对农田建立不伤害作物的防护范围。', '虫害防治', 'plant', '阻止半径 5 格内的兔子破坏成熟作物 60 秒', '蜘蛛眼、花盆与红石粉'),
      P('canopy-cycle', '林冠循环', '记录树叶、木材与树脂的可再生比例。', '林冠循环', 'research', '统计半径 12 格内树叶破坏次数并在达到 32 次时提示补种', '书、树苗与红石粉'),
    ],
  }),
  D({
    id: 'defense', stage: 1, name: '防御学', status: STATUS.implemented,
    tagline: '将生存风险拆解为火焰、坠落、袭击与据点防护。',
    overview: '防御学通过已存在的烈焰甲与跳跃靴建立装备基础，并规划面对环境和据点的防护体系。',
    learningGoals: ['按风险来源选择护甲与机动装备。', '理解防火、坠落减伤和据点防线的差别。', '在探索前准备可撤离的防护配置。'],
    families: [
      E('heat-armor', '耐火护甲', '现有烈焰甲抵御自然火焰伤害。', [
        I('fire-chestplate', '烈焰甲', 'II', '防具', '穿戴后抵御自然火焰伤害，并减轻部分自然伤害。', '按已注册烈焰甲配方制作', STATUS.implemented),
        I('fire-greaves', '烈焰护胫', 'III', '防具', '穿戴后将岩浆接触伤害延后 1 秒结算。', '火焰锭、铁质护腿与皮革在高级工作台组装'),
        I('fire-bastion', '烈焰壁垒', 'III', '防御设施', '放置并激活后让半径 4 格内玩家获得 5 秒抗火效果。', '火焰块、黑曜石与红石粉在高级工作台组装'),
      ]),
      E('mobility-armor', '机动防具', '现有跳跃靴在空中蹲下时提供跃迁。', [
        I('jumper-boots', '跳跃靴', 'II', '防具', '穿戴后在空中蹲下可向前方跃迁，帮助跨越短距离空隙。', '羽毛、树脂与火焰块按已注册配方制作', STATUS.implemented),
        I('landing-plate', '缓冲落地板', 'III', '防具部件', '装配到靴子后将一次超过 6 格的坠落伤害降低 20%。', '树脂、强力丝线与铁锭在高级工作台组装'),
        I('escape-beacon', '撤离信标', 'III', '防御设施', '放置后在玩家生命低于 4 点时发出红色粒子与钟声。', '红石灯、铁锭与末影珍珠在高级工作台组装'),
      ]),
      P('shielding', '盾面工艺', '构建能判断正面冲击的可维修盾面。', '盾面工艺', 'defense', '在面对攻击方向时将一次近战伤害降低 2 点', '木板、铁锭与皮革'),
      P('ranged-cover', '远程掩体', '为远程伤害提供有方向的阻挡结构。', '远程掩体', 'construction', '阻挡来自正面 120 度扇区的第一支箭矢', '圆石、橡木板与铁锭'),
      P('alarm-grid', '警戒网', '把入侵事件转化为清晰的声光反馈。', '警戒网', 'defense', '在未授权玩家进入半径 8 格时播放钟声并显示橙色粒子', '线、红石粉与钟'),
      P('healing-post', '急救站', '为战斗结束后的恢复建立固定节点。', '急救站', 'defense', '每 40 tick 为半径 3 格内非战斗玩家恢复 1 点生命', '金苹果、床与红石粉'),
      P('hazard-suit', '危害防护', '在有毒、低氧与粉尘环境中维持可见状态。', '危害防护', 'defense', '将仙人掌、甜浆果与粉雪造成的每次伤害降低 1 点', '皮革、玻璃与树脂'),
      P('fortification', '据点加固', '建立可检测墙体破坏的防线。', '据点加固', 'construction', '在相邻墙体被破坏时向所有者发送方块坐标提示', '圆石、红石比较器与书'),
      P('rescue-line', '救援绳索', '连接高低处路径以降低意外坠落代价。', '救援绳索', 'defense', '右键锚点后在 12 格内生成可攀爬绳索 30 秒', '强力丝线、木棍与铁锭'),
      P('ward-training', '防御训练', '把反应、格挡和撤离写成可重复演练。', '防御训练', 'research', '记录 60 秒内受到的伤害来源并按类型生成摘要', '书、盾牌与红石粉'),
    ],
  }),
  D({
    id: 'technology', stage: 1, name: '科技', status: STATUS.implemented,
    tagline: '以导线、设备与五类机器建立可观察的生产基础。',
    overview: '科技收拢现有电气部件、火力发电、储能、储罐与五类已注册机器，为后续自动化提供接口。',
    learningGoals: ['读懂导线、发电、储能与负载的基本关系。', '在正确工位执行压缩、破碎、冶炼、筛分与高级合成。', '使用储罐和工具维护机器工作区。'],
    families: [
      E('electric-components', '电气部件', '现有铁质导线、电路板与扳手连接并维护电网。', [
        I('iron-wire', '铁质导线', 'I', '电气部件', '放置后连接电网节点，单段线路损耗为 0.5%。', '在已注册电气配方中制作', STATUS.implemented),
        I('circuit-board', '电路板', 'II', '电气部件', '作为机器与电力设备的逻辑部件，承载已注册设备配方。', '在已注册电气配方中制作', STATUS.implemented),
        I('wrench', '扳手', 'II', '维护工具', '对机器使用后执行拆卸维护，不回显设备内部数据。', '在基础工作台按扳手配方制作', STATUS.implemented),
      ]),
      E('thermal-generation', '火力发电', '现有火力发电机将燃料转化为灵魂电能。', [
        I('fire-generator', '火力发电机', 'II', '发电设备', '放置后消耗燃料并稳定产生灵魂电能，供相连设备使用。', '按已注册火力发电机配方制作', STATUS.implemented),
        I('draft-controller', '风量控制器', 'III', '电气部件', '装入火力发电机后在燃料不足时停止点火并显示黄色提示。', '铁锭、红石比较器与电路板在高级工作台组装'),
        I('ash-collector', '灰烬收集器', 'III', '机器部件', '装入火力发电机后每消耗 16 个燃料收集 1 份灰烬材料。', '漏斗、铁锭与火焰锭在高级工作台组装'),
      ]),
      E('energy-storage', '基础储能', '现有基础蓄电池储存富余电量并在供电不足时补充。', [
        I('basic-battery', '基础蓄电池', 'II', '储能设备', '连接电网后储存富余电量，并在发电不足时自动补充消费者。', '按已注册基础蓄电池配方制作', STATUS.implemented),
        I('charge-meter', '充电计量器', 'III', '电气部件', '接入蓄电池后每 20 tick 显示当前电量与容量百分比。', '电路板、玻璃与红石粉在高级工作台组装'),
        I('power-isolator', '电网隔离器', 'III', '电气设备', '红石信号开启时断开相连两侧的能量传输。', '铁质导线、红石中继器与铁锭在高级工作台组装'),
      ]),
      E('fluid-tank', '储罐', '现有基础储罐记录液体、容量与存储模式。', [
        I('basic-tank', '基础储罐', 'II', '流体设备', '存储液体并显示当前液量、容量与存储模式，左键切换模式。', '按已注册基础储罐配方制作', STATUS.implemented),
        I('level-gauge', '液位计', 'III', '流体部件', '安装到储罐后在液量低于 20% 时显示蓝色粒子。', '玻璃瓶、红石比较器与铁锭在高级工作台组装'),
        I('spill-tray', '防溢托盘', 'III', '流体部件', '放置在储罐下方时把溢出的第一桶液体保留在托盘中。', '铁锭、玻璃与黏液球在高级工作台组装'),
      ]),
      E('registered-machines-a', '已注册机器', '现有高级工作台、破碎锤与压缩机覆盖三类基础作业。', [
        I('advanced-workbench', '高级工作台', 'II', '机器', '打开后执行已注册的高级合成配方。', '按已注册高级工作台结构制作', STATUS.implemented),
        I('break-hammer-machine', '破碎锤机', 'II', '机器', '放置后引导玩家手持破碎锤敲击指定物品完成破碎作业。', '按已注册破碎锤机结构制作', STATUS.implemented),
        I('compressor', '压缩机', 'II', '机器', '放入 9 个同类物品后执行压缩作业。', '按已注册压缩机结构制作', STATUS.implemented),
      ]),
      E('registered-machines-b', '已注册机器工位', '现有冶炼锅炉与过滤筛子补全两类基础处理。', [
        I('furnace-cauldron', '冶炼锅炉', 'II', '机器', '在锅炉下点火后处理已注册冶炼配方。', '按已注册冶炼锅炉结构制作', STATUS.implemented),
        I('filtering-sieve', '过滤筛子', 'II', '机器', '将筛网放在漏斗上形成筛子，并执行已注册筛分配方。', '按已注册过滤筛子结构制作', STATUS.implemented),
        I('machine-console', '机器维护台', 'III', '机器', '放置后显示半径 4 格内机器的名称、停机状态与维护提示。', '工作台、书与电路板在高级工作台组装'),
      ]),
      P('signal-basics', '信号基础', '把红石状态转换为机器可读的开停指令。', '信号基础', 'energy', '在接到红石信号时把相连机器切换为安全停机状态', '红石粉、红石中继器与电路板'),
      P('machine-core', '机器核心', '为可放置设备提供统一的工作与停机边界。', '机器核心', 'machine', '在方块被拆除前保存工作状态并阻止未授权玩家启动设备', '观察者、铁锭与红石粉'),
      P('cobble-production', '刷石生产', '把熔岩与水的交界转为计量产出的基础石材。', '刷石生产', 'machine', '每 10 秒在输出槽生成 1 块圆石并在满槽时停止', '发射器、熔岩桶、水桶与红石粉'),
      P('machine-network', '机器网络', '用状态板观测多台设备的运行关系。', '机器网络', 'research', '显示半径 16 格内机器的供能、运行与停机数量', '书、红石灯与电路板'),
    ],
  }),
  D({
    id: 'magic', stage: 1, name: '魔法', status: STATUS.implemented,
    tagline: '以法杖、展示台与注魔核心让魔能拥有可见的载体。',
    overview: '魔法学收拢现有魔力手杖、恐怖手杖、魔力展示台与注魔核心，并规划可解释的施法与仪式边界。',
    learningGoals: ['识别法杖、注魔核心和展示台的职责。', '在施法前检查魔能与目标范围。', '将魔法效果限制在明确的方块和实体边界内。'],
    families: [
      E('wand-core', '法杖与注魔', '现有法杖和注魔核心构成魔能使用基础。', [
        I('magic-wand', '魔力手杖 I', 'II', '法术工具', '手持施放时消耗并显示法杖的魔能储量。', '按已注册魔力手杖配方制作', STATUS.implemented),
        I('mystery-wand', '恐怖手杖 I', 'III', '法术工具', '保留历史的无限魔能标记，仅用于 legacy 法杖效果；不向电网、运输或跨域作业提供能量或成本来源。', '按已注册恐怖手杖配方制作', STATUS.implemented),
        I('injection-core', '注魔核心', 'III', '魔法核心', '作为注魔配方的核心输入，承载物品的魔能注入过程。', '按已注册注魔核心配方制作', STATUS.implemented),
      ]),
      E('display-ritual', '魔力展示', '现有魔力展示台可呈现放入的物品。', [
        I('magic-display', '魔力展示台', 'II', '魔法设施', '放置后展示指定物品，让玩家观察魔法物品的外观与状态。', '按已注册魔力展示台配方制作', STATUS.implemented),
        I('glyph-lens', '符文透镜', 'III', '法术材料', '嵌入展示台后显示其中物品的魔能等级与持续时间。', '玻璃、青金石与注魔核心在高级工作台组装'),
        I('ritual-bell', '仪式钟', 'III', '魔法设施', '放置并注入魔能后每 40 tick 播放一次仪式提示音。', '钟、紫水晶与火焰锭在高级工作台组装'),
      ]),
      P('mana-measurement', '魔能计量', '把法术消耗量转换为可读数值。', '魔能计量', 'magic', '显示最近一次施法消耗的魔能数值并持续 5 秒', '紫水晶、红石粉与玻璃'),
      P('elemental-runes', '元素符文', '把火、风、水、土效果分隔为可组合符文。', '元素符文', 'magic', '让目标方块在 4 秒内显示对应元素颜色的粒子', '染料、石英与纸'),
      P('ward-circle', '守护圆环', '建立不直接伤害实体的区域提示法阵。', '守护圆环', 'magic', '在半径 5 格内显示紫色圆形边界并提示未授权玩家离开', '紫水晶、红石粉与金锭'),
      P('enchanted-tools', '附魔工具', '让工具效果通过明确触发条件生效。', '附魔工具', 'magic', '在耐久高于 50% 时让一次方块破坏额外掉落 1 点经验', '青金石、铁锭与经验瓶'),
      P('soul-binding', '灵魂绑定', '为个人装备建立不可混淆的归属标记。', '灵魂绑定', 'magic', '在非所有者尝试使用时取消操作并显示所有者名称', '灵魂沙、书与末影珍珠'),
      P('arcane-garden', '奥术花园', '让魔法作物与普通作物的生长规则保持分离。', '奥术花园', 'magic', '阻止超级骨粉对半径 3 格内标记作物直接催熟', '紫水晶、花与耕地'),
      P('spell-practice', '施法演练', '通过无伤害目标练习施法范围和冷却。', '魔能演练', 'research', '记录 30 秒内施法方向并绘制不造成伤害的蓝色轨迹', '书、木棍与红石粉'),
      P('magic-storage', '魔能储存', '把闲置魔能写入安全容器。', '魔能储存', 'magic', '在法杖未使用 60 秒后保存其当前魔能并阻止自然衰减', '紫水晶块、玻璃与注魔核心'),
    ],
  }),
  D({
    id: 'space', stage: 1, name: '空间', status: STATUS.implemented,
    tagline: '从空间碎片与末影粉尘出发，建立有坐标边界的移动。',
    overview: '空间学使用现有空间碎片和末影粉尘作为研究入口，规划位置绑定、路径记录与安全回程。',
    learningGoals: ['区分材料碎片、坐标锚与传送设施。', '在启用移动前记录起点与终点坐标。', '为跨区域移动保留可用的回程路径。'],
    families: [
      E('space-dust', '空间尘', '现有空间碎片与末影粉尘提供空间配方输入。', [
        I('space-fragment', '空间碎片', 'II', '空间材料', '作为虚空波动能量材料，用于空间学的已注册配方。', '在已注册空间材料配方中获得', STATUS.implemented),
        I('end-stone-dust', '末影粉尘', 'II', '空间材料', '作为末地来源的微弱空间能量材料，用于空间学的已注册配方。', '在已注册空间材料配方中获得', STATUS.implemented),
        I('coordinate-chalk', '坐标粉笔', 'III', '空间工具', '右键方块后记录其坐标，并在 60 秒内显示一条白色粒子指引线。', '空间碎片、末影粉尘与白色染料在高级工作台组装'),
      ]),
      P('local-anchor', '本地锚定', '把一个明确坐标设为短距移动的返回点。', '本地锚定', 'space', '把玩家位置绑定为返回坐标并在 30 分钟内保持有效', '空间碎片、铁锭与指南针'),
      P('route-record', '路径记录', '记录地表与地下路径的安全节点。', '路径记录', 'research', '每经过 16 格自动写入一个带维度名的路径节点', '纸、指南针与空间碎片'),
      P('portal-frame', '门框校准', '校验门框尺寸、朝向与周边阻挡。', '门框校准', 'space', '在门框缺失方块时显示红色粒子并拒绝激活', '黑曜石、空间碎片与红石粉'),
      P('cargo-folding', '货物折叠', '以受限次数减少搬运往返。', '货物折叠', 'space', '将指定箱子的前 9 格物品转移到绑定箱子并消耗 1 次充能', '箱子、末影珍珠与空间碎片'),
      P('rift-safety', '裂隙安全', '监控传送目标附近是否存在可站立方块。', '裂隙安全', 'space', '在目标坐标下方 3 格没有实体方块时取消移动并返回原位', '黑曜石、铁锭与羽毛'),
      P('dimension-map', '维度地图', '用坐标和门户连接形成可审计地图。', '维度地图', 'research', '显示已绑定门户之间的维度名、坐标和最后使用时间', '地图、书与末影粉尘'),
      P('space-cache', '空间缓存', '防止跨区块移动时的临时物品丢失。', '空间缓存', 'space', '在传送开始时冻结玩家手持物品并在到达后恢复原数量', '箱子、空间碎片与红石比较器'),
      P('return-signal', '返航信号', '为探索者提供一条不可误触发的返回提示。', '返航信号', 'space', '在距离绑定锚点超过 256 格时每 10 秒显示方向箭头', '指南针、空间碎片与钟'),
      P('void-observer', '虚空观测', '观察脚下空洞与坠落风险。', '虚空观测', 'research', '在玩家下方 16 格没有固体方块时显示紫色警告粒子', '望远镜、末影粉尘与玻璃'),
    ],
  }),
  D({
    id: 'gravity', stage: 1, name: '引力', status: STATUS.implemented,
    tagline: '将万有引力作为移动、收集与结构稳定的可调变量。',
    overview: '引力学是现有八根学科之一，后续内容将以范围、方向和强度三个参数约束引力效果。',
    learningGoals: ['读懂引力方向、半径和强度的限制。', '避免把实体牵引效果用于无边界区域。', '在机器与建筑中使用明确的重量阈值。'],
    families: [
      P('mass-reading', '质量读数', '读取方块和实体的重量等级。', '质量读数', 'gravity', '显示目标方块的轻型、中型或重型重量标签并持续 8 秒', '铁锭、红石粉与玻璃'),
      P('item-pull', '物品牵引', '在可见范围内收集散落物品。', '物品牵引', 'gravity', '将半径 4 格内的掉落物缓慢拉向使用者且不穿过墙体', '铁锭、末影珍珠与红石粉'),
      P('fall-buffer', '坠落缓冲', '在落地前短暂降低下落速度。', '坠落缓冲', 'gravity', '在玩家下落速度超过每 tick 0.8 格时将速度降低 30%', '羽毛、树脂与铁锭'),
      P('load-platform', '载荷平台', '让机械作业受到可读重量限制。', '载荷平台', 'gravity', '当容器物品数超过 64 时停止相连机器并显示红色粒子', '铁块、红石比较器与木板'),
      P('ore-separation', '重力分选', '按重量把原料分成两个输出槽。', '重力分选', 'machine', '把矿石原料的前 32 个分配到轻料槽或重料槽并记录数量', '漏斗、铁锭与红石粉'),
      P('structure-balance', '结构平衡', '检查悬挑结构的支撑距离。', '结构平衡', 'construction', '在方块距离最近支撑超过 6 格时显示橙色粒子', '圆石、铁锭与线'),
      P('gravity-lens', '引力透镜', '观测引力场在固定范围内的变化。', '引力透镜', 'research', '每 20 tick 显示半径 5 格内实体的移动方向箭头', '玻璃、紫水晶与铁锭'),
      P('lift-column', '升力柱', '创建上行且不造成摔落的短距通道。', '升力柱', 'gravity', '使半径 1 格内玩家以每 tick 0.25 格速度上升且在离开时缓慢落地', '灵魂沙、铁锭与红石粉'),
      P('anchor-weight', '锚定配重', '将设备固定到单个可验证位置。', '引力配重', 'gravity', '阻止未授权玩家推动或拆除绑定设备 30 秒', '铁块、锁链与红石粉'),
      P('field-calibration', '场强校准', '把引力设备的范围限制写入调校记录。', '场强校准', 'research', '记录引力设施的半径和强度并在超过设定值时自动停机', '书、铁锭与红石比较器'),
    ],
  }),
  D({
    id: 'geology', stage: 2, name: '地质学',
    tagline: '以岩层、矿脉和洞穴结构指导更安全的资源采集。',
    overview: '地质学将地形从随机障碍转为可测量样本，规划矿脉定位、岩层支撑与深层风险识别。',
    learningGoals: ['根据岩层判断矿脉与空洞风险。', '在开采前建立支撑和路径标记。', '把采样结果转换为可复用的采矿决策。'],
    families: [
      P('strata-survey', '地层测绘', '记录岩层厚度与矿脉方向。', '地层测绘', 'research', '将半径 12 格内同类矿石标记为蓝色轮廓并保持 8 秒', '圆石、红石粉与指南针'),
      P('ore-prospecting', '矿脉勘探', '在不破坏方块前判断潜在矿脉走向。', '矿脉勘探', 'research', '显示最近一个矿石方块的方向和距离，最大距离 24 格', '铁锭、青金石与指南针'),
      P('fault-extraction', '断层开采', '避开会造成坍塌的裂隙边缘。', '断层开采', 'machine', '在检测到相邻 3 格空气裂隙时暂停钻取并显示红色粒子', '铁锭、木板与红石比较器'),
      P('crystal-growth', '晶体生长', '在稳定岩壁上培育可采集晶体。', '晶体培育', 'resource', '每 600 tick 在标记石英岩壁生成 1 个可采集晶体', '紫水晶、石英与水瓶'),
      P('cave-support', '洞穴支护', '为地下通道提供可读承重边界。', '洞穴支护', 'construction', '使相邻 4 格顶板获得 30 秒黄色支撑标记', '原木、铁锭与圆石'),
      P('fossil-reading', '化石解读', '从骨块与岩层记录远古环境。', '化石解读', 'research', '右键骨块后显示其所在高度与周围石材类型', '骨块、书与红石粉'),
      P('volcanic-sampling', '火山取样', '识别高温岩浆边缘的安全采样点。', '火山取样', 'processing', '在岩浆边缘 2 格内显示可站立方块并保持 10 秒', '火焰锭、玻璃瓶与铁锭'),
      P('karst-water', '岩溶水文', '判断地下水流对洞穴的影响。', '岩溶水文', 'fluid', '显示半径 8 格内水源方块与流动方块的数量', '玻璃瓶、红石粉与石英'),
      P('deep-core', '深层岩芯', '提取深处岩层样本以判断开采深度。', '深层岩芯', 'resource', '记录使用位置的高度并在低于 Y=0 时显示紫色深层标记', '铁锭、圆石与紫水晶'),
      P('terrain-model', '地形模型', '把采样点拼成可读的地下模型。', '地形模型', 'research', '显示最近 16 个采样点的连线与高度差', '地图、纸与红石粉'),
    ],
  }),
  D({
    id: 'metallurgy', stage: 2, name: '冶金学',
    tagline: '把矿石变成成分、热处理和精度都可控制的金属构件。',
    overview: '冶金学规划洗矿、合金、铸造、锻造与回收，让金属从单一锭材进入多步骤生产链。',
    learningGoals: ['区分矿石处理、熔炼和成型工位。', '按用途选择耐热、耐腐蚀和高强度材料。', '让废料重新回到明确的材料链。'],
    families: [
      P('ore-washing', '洗矿分级', '去除原矿中的无用杂质。', '洗矿分级', 'processing', '每处理 8 个原矿输出 6 个净矿与 2 个石屑', '水桶、铁锭与砂砾'),
      P('smelting-control', '熔炼控温', '以固定温度窗口减少冶炼失败。', '熔炼控温', 'processing', '当熔炉温度偏离设定区间时停止输入并显示橙色粒子', '火焰锭、红石比较器与铁锭'),
      P('alloying', '合金配比', '将两种金属按比例制成用途明确的合金。', '合金配比', 'resource', '按 2 比 1 消耗两种金属并输出 1 个稳定合金', '铁锭、铜锭与石英'),
      P('casting', '模具铸造', '把熔融金属导向可重复使用的模具。', '模具铸造', 'machine', '每次作业把 1 桶金属液转换为 1 个定型构件', '黏土、铁锭与玻璃'),
      P('forging', '锻压成型', '用压力塑造高强度部件。', '锻压成型', 'machine', '每次作业将金属坯压成 1 个耐久高于普通锭的零件', '铁块、活塞与火焰锭'),
      P('heat-treatment', '热处理', '用淬火和回火改变金属用途。', '热处理', 'processing', '在冷却液存在时把构件的耐久消耗降低 10%', '水桶、火焰锭与铁锭'),
      P('plating', '表面镀层', '为装备和设施附加薄层保护。', '表面镀层', 'processing', '对目标装备使用后使其第一次酸性伤害降低 2 点', '金锭、树脂与玻璃瓶'),
      P('corrosion', '腐蚀防护', '监控液体与金属接触造成的损耗。', '腐蚀防护', 'research', '在储罐液体不匹配时显示绿色警告并停止传输', '铁锭、玻璃与红石粉'),
      P('recycling', '金属回收', '从报废部件中提取可用金属。', '金属回收', 'machine', '每拆解 4 个损坏金属部件返还 1 个基础金属锭', '剪刀、铁锭与煤炭'),
      P('precision-parts', '精密部件', '为高阶机器提供尺寸稳定的构件。', '精密部件', 'machine', '使相连机器的单次作业时间减少 1 tick，最低不低于 5 tick', '铁锭、石英与电路板'),
    ],
  }),
  D({
    id: 'chemistry', stage: 2, name: '化学工程',
    tagline: '让溶液、催化和反应容器拥有可观察的安全边界。',
    overview: '化学工程规划试剂、催化、溶剂、颜料与聚合物，所有反应均以输入、容器和产物为明确边界。',
    learningGoals: ['识别反应物、容器和安全停机条件。', '按颜色和状态读出化学工艺的结果。', '把试剂限制在指定机器和作业范围内。'],
    families: [
      P('acid-base', '酸碱处理', '用中和反应处理特定液体。', '酸碱处理', 'processing', '将储罐中 1 桶标记酸液转化为 1 桶中性液并消耗试剂', '玻璃瓶、石灰与水桶'),
      P('salt-crystal', '盐类结晶', '从浓缩溶液析出可运输晶体。', '盐类结晶', 'processing', '每 200 tick 从满液储罐析出 1 个盐晶体', '水桶、石英与玻璃'),
      P('catalyst', '催化反应', '以可计数催化剂缩短稳定反应。', '催化反应', 'processing', '使相连反应釜的单次作业时间减少 2 tick并消耗 1 点催化耐久', '铁锭、红石粉与紫水晶'),
      P('gas-capture', '气体收集', '把设备排放导入封闭容器。', '气体收集', 'fluid', '每 40 tick 将相邻反应设备的 1 单位气体写入气瓶', '玻璃瓶、铁锭与漏斗'),
      P('solvent-cycle', '溶剂循环', '回收反应后的可再用液体。', '溶剂循环', 'fluid', '每处理 4 桶反应液返还 1 桶可再用溶剂', '水桶、玻璃与煤炭'),
      P('polymer', '聚合材料', '将树脂制成定型的弹性材料。', '聚合材料', 'processing', '把 4 份树脂转化为 1 个可承受 8 次弯折的部件', '树脂、火焰锭与强力丝线'),
      P('pigment', '功能颜料', '用颜色表达可识别的设备状态。', '功能颜料', 'processing', '对机器涂装后在运行、待机和故障时显示三种固定颜色', '染料、玻璃瓶与树脂'),
      P('mining-paste', '采矿膏', '在指定方块表面形成短时标记。', '采矿膏', 'processing', '右键石材后使其显示 10 秒黄色边缘且不破坏方块', '黏液球、砂砾与红石粉'),
      P('water-analysis', '水质分析', '读出流体网络中液体的种类和比例。', '水质分析', 'research', '显示相连储罐前两种液体名称及其容量百分比', '玻璃瓶、书与红石粉'),
      P('lab-safety', '实验室安全', '在反应条件失配时留下明确的反馈。', '实验室安全', 'research', '当反应釜缺少容器或输入错误时锁定作业并播放一次钟声', '钟、红石比较器与玻璃'),
    ],
  }),
  D({
    id: 'agroecology', stage: 2, name: '农业生态',
    tagline: '让土地、水、授粉和轮作形成长期可维护的生产循环。',
    overview: '农业生态把单株种植扩展到田块管理，规划土壤、灌溉、育种和害虫防护的可持续玩法。',
    learningGoals: ['按田块管理水分、肥力和作物类型。', '通过轮作与堆肥维持长期产量。', '在不伤害作物的前提下防止生物破坏。'],
    families: [
      P('soil-restoration', '土壤修复', '恢复连续耕作后的土地状态。', '土壤修复', 'plant', '使半径 3 格内的耕地恢复湿润并清除一次踩踏标记', '泥土、骨粉与水瓶'),
      P('irrigation', '灌溉布局', '把水源稳定送入田块。', '灌溉布局', 'fluid', '每 20 tick 为半径 4 格内干燥耕地补充湿润状态', '水桶、铁锭与木板'),
      P('pollinator', '授粉群落', '让花与作物之间形成可见联系。', '授粉群落', 'plant', '每 400 tick 为半径 6 格内一株成熟作物额外生成 1 个种子', '蜂蜜瓶、花与玻璃'),
      P('compost-loop', '堆肥循环', '把农作物副产物送回土地。', '堆肥循环', 'processing', '每消耗 16 个植物掉落物为最近耕地增加 1 次肥力', '堆肥桶、树叶与骨粉'),
      P('graft-bed', '嫁接苗床', '在可控环境中复制作物特性。', '嫁接苗床', 'plant', '使放入的两种同类种子在 600 tick 后输出 1 个强化种子', '木板、剪刀与黏液球'),
      P('pest-control', '虫害防治', '以非伤害方式保护成熟作物。', '虫害防治', 'plant', '阻止半径 5 格内兔子破坏成熟作物并持续 60 秒', '蜘蛛眼、花盆与红石粉'),
      P('greenhouse-climate', '温室气候', '为不同作物建立稳定温湿度。', '温室气候', 'plant', '使玻璃屋内作物在夜晚保持白色生长粒子 120 秒', '玻璃、铁锭与水桶'),
      P('animal-breeding', '畜牧育种', '记录动物繁殖的冷却状态。', '畜牧育种', 'research', '右键动物后显示其下一次可繁殖的剩余秒数', '书、麦子与红石粉'),
      P('crop-rotation', '轮作规划', '防止同一种作物连续占用田块。', '轮作规划', 'research', '在同一耕地连续种植同种作物三次时显示橙色提示', '纸、种子与红石粉'),
      P('apiary', '蜂房管理', '为蜂蜜与授粉提供固定站点。', '蜂房管理', 'plant', '每 600 tick 在相邻花朵存在时收集 1 瓶蜂蜜且不伤害蜜蜂', '蜂箱、玻璃瓶与花'),
    ],
  }),
  D({
    id: 'hydrology', stage: 2, name: '水利工程',
    tagline: '以储、滤、泵、渠和闸控制每一单位液体的去向。',
    overview: '水利工程规划液体网络、灌溉与防洪，让水、蒸汽、盐水和冰的处理具有清晰容量与流向。',
    learningGoals: ['识别水源、流体容量和管网方向。', '为农田、机器和建筑分配不同液体用途。', '在洪水与堵塞前设置可见警报。'],
    families: [
      P('source-capture', '水源取用', '把带 sourceId 的有限水源账本接入受限管网。', '水源取用', 'fluid', '每次转移先从同一 sourceId 的有限 source ledger 扣除配额；容量、质量或能量不足时停机。原版水方块不删除，但不能重复 credit。', '铁锭、玻璃与水桶'),
      P('filtration', '净水过滤', '移除液体中的颗粒标记。', '净水过滤', 'fluid', '每处理 1 桶浑水输出 1 桶净水并消耗 1 点滤芯耐久', '沙子、木炭与玻璃'),
      P('pressure-pump', '压力泵送', '让液体跨越有限高度差。', '压力泵送', 'fluid', '每 20 tick 将 1 单位液体提升至上方 8 格内的连接管道', '铁锭、活塞与红石粉'),
      P('canal-routing', '渠道分流', '把水流分配给两个明确出口。', 'fluid 分流', 'fluid', '当主储罐超过 50% 时将液体优先送往右侧输出管', '铁锭、红石比较器与玻璃'),
      P('reservoir', '蓄水调度', '在干旱和暴雨间保留缓冲容量。', '蓄水调度', 'fluid', '将储罐液量维持在 25% 至 75% 区间并在越界时停止泵送', '玻璃、铁锭与红石比较器'),
      P('steam-loop', '蒸汽循环', '将热量转换为可输送的工作介质。', '蒸汽循环', 'energy', '每 20 tick 消耗 1 单位水并向相连机组提供 2 点热能', '火焰锭、水桶与铁锭'),
      P('salt-water', '盐水处理', '把盐水分成盐晶与可用水。', '盐水处理', 'processing', '每 200 tick 从 1 桶盐水输出 1 个盐晶与半桶净水', '水桶、石英与玻璃'),
      P('ice-storage', '冰蓄冷', '用冰块稳定需要低温的作业。', '冰蓄冷', 'fluid', '使相邻处理设备的温度上限降低 2 点并持续 120 秒', '冰块、铁锭与玻璃'),
      P('floodgate', '防洪闸门', '在水位升高时保护工作区入口。', '防洪闸门', 'construction', '当相邻水方块数量超过 8 时关闭闸门并显示蓝色粒子', '铁块、活塞与红石比较器'),
      P('leak-monitor', '漏损监测', '发现管网中无目标的液体消耗。', '漏损监测', 'research', '在 60 tick 内液量下降超过 10 单位时记录坐标并播放钟声', '书、红石比较器与玻璃'),
    ],
  }),
  D({
    id: 'logistics', stage: 2, name: '物流学',
    tagline: '让每一件物品拥有来源、去向、优先级与可追溯标签。',
    overview: '物流学规划分类、输送、库存和返程，避免自动化系统将物品混入不可解释的容器。',
    learningGoals: ['为物品分配稳定的分类标签。', '设置输入、输出和满载时的停机边界。', '追踪物品在多个容器间的移动路径。'],
    families: [
      P('sort-tags', '分类标签', '为箱子和物品建立可读分类。', '分类标签', 'logistics', '把与标签同名的物品路由到指定容器并拒绝其他物品', '纸、染料与红石粉'),
      P('belt-line', '传送带线', '用可视方向搬运固定数量的物品。', '传送带线', 'logistics', '每 20 tick 将前方容器的 1 个物品送入后方容器', '皮革、铁锭与木板'),
      P('pneumatic-tube', '气动管道', '跨越短距离输送不易堆叠的物品。', '气动管道', 'logistics', '每 40 tick 将 1 个非堆叠物品送至 12 格内的绑定出口', '玻璃、铁锭与活塞'),
      P('warehouse', '仓储分区', '以容量阈值管理多个容器。', '仓储分区', 'logistics', '在任一绑定箱子超过 80% 容量时停止接收该分类物品', '箱子、红石比较器与书'),
      P('dispatch', '订单派发', '按优先级向作业机投料。', '订单派发', 'logistics', '每 20 tick 向最高优先级且缺料的机器投放 1 个输入物品', '漏斗、电路板与红石粉'),
      P('rail-cargo', '轨道货运', '把物资沿固定站点运输。', '轨道货运', 'logistics', '矿车进入站点后卸载前 9 格物品并等待 40 tick', '铁轨、漏斗与箱子'),
      P('portal-logistics', '门户物流', '在有容量限制的两个仓点间移动货物。', '门户物流', 'logistics', '每 200 tick 将绑定容器前 3 格物品发送到目标容器并记录数量', '末影珍珠、箱子与空间碎片'),
      P('inventory-ledger', '库存账本', '把库存变动记录为可查询事件。', '库存账本', 'research', '每次物品跨容器移动时记录名称、数量、来源与去向', '书、羽毛笔与红石粉'),
      P('return-route', '退货路线', '处理不符合输入要求的物品。', '退货路线', 'logistics', '将机器拒收的物品送回标记为退货的容器且不丢弃', '漏斗、红石中继器与箱子'),
      P('jam-alarm', '堵塞告警', '在物流停止时提供可见告警。', '堵塞告警', 'logistics', '当容器 100 tick 未发生物品移动时播放钟声并显示红色粒子', '钟、红石比较器与玻璃'),
    ],
  }),
  D({
    id: 'construction', stage: 2, name: '建造学',
    tagline: '把建筑从堆叠方块升级为能承重、照明、疏散和维护的设施。',
    overview: '建造学规划框架、桥梁、脚手架、照明和天气防护，服务于长期运行的工坊与聚落。',
    learningGoals: ['识别结构支撑、功能空间和装饰的区别。', '在高处和地下工程中安排安全通道。', '把照明与防护视为设施的一部分。'],
    families: [
      P('structural-frame', '结构框架', '用标准框架表达承重关系。', '结构框架', 'construction', '在连续 4 个框架连接后显示绿色承重粒子', '原木、铁锭与圆石'),
      P('modular-wall', '模块墙体', '用可替换面板组织空间边界。', '模块墙体', 'construction', '在墙体被破坏时保留相邻框架并显示维修坐标', '石砖、木板与树脂'),
      P('bridgework', '桥梁工程', '为跨沟壑路径提供明确支撑。', '桥梁工程', 'construction', '在跨度不超过 12 格时生成蓝色通行边界并提示两端锚点', '木板、铁锭与强力丝线'),
      P('excavation', '定向开挖', '限制挖掘范围以避免误伤结构。', '定向开挖', 'machine', '每次作业只破坏前方 1 格指定石材并把掉落物送入输出槽', '铁锭、活塞与红石粉'),
      P('scaffold', '脚手架作业', '为高处施工提供可回收支撑。', '脚手架作业', 'construction', '放置后允许玩家攀爬 16 格高度并在拆除时返还 1 个框架', '竹子、强力丝线与木板'),
      P('work-light', '工地照明', '为作业区提供不遮挡视线的光源。', '工地照明', 'construction', '在半径 6 格内维持亮度 9 并在机器故障时闪烁红光', '红石灯、玻璃与铁锭'),
      P('signal-safety', '信号安全', '防止红石误触发关键设施。', '信号安全', 'construction', '只有连续 2 个相同红石信号到达时才允许机器启动', '红石中继器、铁锭与电路板'),
      P('weatherproof', '防风雨层', '保护户外设施免受天气影响。', '耐候层', 'construction', '使相邻机器在下雨时的工作效率不降低', '树脂、玻璃与铁锭'),
      P('settlement-core', '聚落核心', '用可见范围划定工坊与公共区域。', '聚落核心', 'construction', '显示半径 16 格的聚落边界并记录已授权玩家', '书、铁块与红石粉'),
      P('landscape', '地形修复', '让开采后的地块恢复可通行状态。', '地形修复', 'construction', '对 3×3 区域使用后将坑洞最低层填平为泥土或圆石', '泥土、圆石与水桶'),
    ],
  }),
  D({
    id: 'energy', stage: 3, name: '能源工程',
    tagline: '从单台火力机走向可调度、可保护、可度量的能量系统。',
    overview: '能源工程规划多来源供能、热量储存、周期通量调配、共享线路保护和负荷响应，避免设备在无反馈时失控。',
    learningGoals: ['读懂发电、储能、损耗和负载四类状态。', '为不同负载配置正确的周期通量与共享线路边界。', '在超载前让系统主动保护而不是静默损坏。'],
    families: [
      P('solid-fuel', '固体燃料', '让燃料热值与消耗速率可被观察。', '固体燃料', 'energy', '每 20 tick 产出固定 2 点热能并记录剩余燃料 tick', '煤炭、木炭与铁锭'),
      P('biomass', '生物质能', '把农业副产物转为低速稳定能源。', '生物质能', 'energy', '每 40 tick 消耗 4 个植物掉落物并产出 1 点能量', '堆肥、木板与电路板'),
      P('steam-turbine', '蒸汽涡轮', '通过蒸汽驱动连续发电。', '蒸汽涡轮', 'energy', '每 20 tick 消耗 1 单位蒸汽并向电网送入 3 点能量', '铁锭、火焰锭与活塞'),
      P('solar-mirror', '太阳镜阵', '在白天将光照转为低功率电能。', '太阳镜阵', 'energy', '在天空可见且白天时每 20 tick 产出 1 点能量', '玻璃、铁锭与红石粉'),
      P('wind-unit', '风力机组', '依据高度和天气提供可变输出。', '风力机组', 'energy', '在 Y=100 以上且下雨时每 20 tick 产出 2 点能量', '木板、铁锭与线'),
      P('heat-storage', '蓄热系统', '把短时过剩热量存为可回收能量。', '蓄热系统', 'energy', '在发电富余时每 20 tick 存入 2 点热能并在缺电时释放', '火焰锭、陶瓷外壳与铁锭'),
      P('battery-bank', '电池阵列', '将多个储能单元组合为容量池。', '电池阵列', 'energy', '把相邻 4 个蓄电池的可用容量汇总为一个读数', '基础蓄电池、铁锭与电路板'),
      P('transformer', '通量调配', '限制周期通量与共享线路吞吐，只使用统一 milli-SE 能量域。', '通量调配', 'energy', '按周期限制 milli-SE 输入与输出通量，容量或共享线路超限时拒绝请求。', '铜锭、铁锭与红石粉'),
      P('grid-protection', '电网保护', '在异常负载时隔离故障支路。', '电网保护', 'energy', '当支路连续 40 tick 超过额定输入时断开该支路', '红石比较器、铁锭与电路板'),
      P('load-response', '负荷响应', '让非关键机器在电量不足时有序停机。', '负荷响应', 'energy', '当储能低于 15% 时按优先级停止低优先级机器', '书、红石粉与电路板'),
    ],
  }),
  D({
    id: 'mechanics', stage: 3, name: '机械工程',
    tagline: '让转动、切削、挤压和输送成为可维护的实体作业。',
    overview: '机械工程规划齿轮、轴承、压力机、粉碎、输送和维护，把单台机器连接为可诊断的生产单元。',
    learningGoals: ['区分动力传递、加工和输送设备。', '在开始作业前检查输入、输出和耐久。', '通过维护而非重复摆放恢复设备状态。'],
    families: [
      P('gear-train', '齿轮传动', '用齿轮组合改变动力方向与速率。', '齿轮传动', 'machine', '将相邻动力轴的方向旋转 90 度且保持每 20 tick 2 点传输上限', '铁锭、木板与红石粉'),
      P('drive-shaft', '动力轴', '在有限距离内传递机械输出。', '动力轴', 'machine', '把动力传至直线 8 格内的连接机器且每段损失 0.1 点', '铁锭、原木与树脂'),
      P('bearing', '耐磨轴承', '减少连续作业时的停机概率。', '耐磨轴承', 'machine', '使相连机械每 200 次作业才消耗 1 点维护耐久', '铁锭、树脂与石英'),
      P('cutter', '切削工位', '对原料执行定量切分。', '切削工位', 'machine', '每次作业将 1 个木材或纤维输入切为 2 个指定部件', '铁锭、木板与电路板'),
      P('press', '压力工位', '将片材压成结构件。', '压力工位', 'machine', '每次作业把 2 个金属片压为 1 个框架部件', '铁块、活塞与红石粉'),
      P('crusher', '粉碎工位', '将硬质材料转为颗粒输入。', '粉碎工位', 'machine', '每次作业将 1 个指定矿石块转为 4 个颗粒且不处理非白名单方块', '铁块、圆石与电路板'),
      P('conveyor', '连续输送', '把机器输出按固定节拍送往下游。', '连续输送', 'machine', '每 20 tick 将输出槽的第一个物品移动到相邻容器', '皮革、铁锭与红石粉'),
      P('robot-arm', '机械臂', '对邻近容器执行可见的取放动作。', '机械臂', 'machine', '每 40 tick 从输入容器取 1 个物品并放入目标槽位', '铁锭、活塞与电路板'),
      P('milling', '精密研磨', '把颗粒加工为一致尺寸的粉末。', '精密研磨', 'machine', '每次作业将 4 个颗粒转为 1 个精制粉末并消耗 1 点耐久', '石英、铁锭与砂砾'),
      P('maintenance', '机械维护', '在故障前给出确定维护窗口。', '机械维护', 'research', '当机器维护耐久低于 10% 时记录坐标并停止新任务', '书、铁锭与红石比较器'),
    ],
  }),
  D({
    id: 'automation', stage: 3, name: '自动控制',
    tagline: '以传感、阈值和安全回退编排机器而不是放任机器运行。',
    overview: '自动控制规划传感器、调度、采收、容错和故障隔离，使工坊在条件变化时保持可解释。',
    learningGoals: ['为自动化设置可观察的触发条件。', '区分正常作业、暂停与故障隔离。', '让自动设备在资源耗尽时安全停止。'],
    families: [
      P('sensors', '状态传感', '读取库存、液位、能量与方块状态。', '状态传感', 'research', '每 20 tick 输出连接设备的库存百分比、液位百分比或电量百分比', '红石比较器、玻璃与电路板'),
      P('thresholds', '阈值逻辑', '用上下限控制设备开停。', '阈值逻辑', 'machine', '当读数低于下限时启动设备，高于上限时停止设备', '红石粉、比较器与电路板'),
      P('scheduler', '任务调度', '按明确顺序为机器分配时间片。', '调度器', 'machine', '每 20 tick 只允许优先级最高的一个等待任务开始', '时钟、书与电路板'),
      P('recipe-control', '配方控制', '防止机器混入不匹配输入。', '配方控制', 'machine', '检测到非当前配方输入时拒绝物品并将其送回退货槽', '漏斗、红石比较器与电路板'),
      P('inventory-watch', '库存观察', '在库存接近耗尽时先停机后提示。', '库存观察', 'research', '当指定物品少于 4 个时停止相连机器并播放一次钟声', '箱子、书与红石粉'),
      P('field-drone', '田间作业', '让有限范围的设备执行单次农田动作。', '田间作业', 'machine', '每 40 tick 对前方 3×3 耕地检查一株成熟作物并收获', '铁锭、剪刀与电路板'),
      P('block-placer', '方块部署', '在白名单方块上执行定向放置。', '方块部署', 'machine', '每 40 tick 将输出槽的第一个白名单方块放到前方空位', '发射器、铁锭与电路板'),
      P('harvester', '自动采收', '将可成熟作物的收获与补种绑定。', '自动采收', 'machine', '每次作业收获 1 株成熟作物并立刻放入同种种子', '剪刀、种子与电路板'),
      P('recovery', '任务恢复', '在区块重新加载后恢复未完成任务。', '自动恢复', 'research', '加载区块时恢复最后一个未完成任务且不重复消耗输入', '书、红石粉与铁锭'),
      P('fault-isolation', '故障隔离', '把异常设备从正常作业链中移除。', '故障隔离', 'machine', '当设备连续三次报错时断开其输入并标记为故障', '红石比较器、铁锭与电路板'),
    ],
  }),
  D({
    id: 'environment', stage: 3, name: '环境工程',
    tagline: '用修复、监测和保护区维持工坊周围的生态承载。',
    overview: '环境工程规划空气、水、土壤、栖息地与天气影响，避免生产链以不可见代价破坏周边世界。',
    learningGoals: ['识别生产活动对水、土和生物的影响。', '设置可量化的修复与保护范围。', '在异常环境指标出现时先停止源头。'],
    families: [
      P('air-quality', '空气监测', '追踪燃烧与粉尘区域的状态。', '空气监测', 'research', '在半径 8 格内燃烧设备超过 3 台时显示灰色粒子', '玻璃、红石粉与铁锭'),
      P('soil-repair', '土壤修复', '恢复机器周边受损耕地。', '土壤修复', 'plant', '每 200 tick 将一块标记为受损的泥土恢复为可耕地', '泥土、骨粉与水瓶'),
      P('water-cleanup', '水体净化', '处理排出到收集槽的废液。', '水体净化', 'fluid', '每处理 1 桶废液输出半桶净水并生成 1 个固体残渣', '木炭、玻璃与水桶'),
      P('waste-processing', '废料处理', '把不同来源的废料导向可回收流程。', '废料处理', 'machine', '每 40 tick 将 1 个标记废料分入金属、植物或石材输出槽', '漏斗、铁锭与电路板'),
      P('habitat', '栖息地恢复', '为生物保留可停留的安全区域。', '栖息地恢复', 'plant', '使半径 6 格内草方块保持可供被动生物生成的状态 600 秒', '草方块、花与水桶'),
      P('weather-tower', '天气观测', '提前显示降雨和雷暴的短时风险。', '天气观测', 'research', '在降雨或雷暴开始前 20 秒显示蓝色或黄色粒子', '避雷针、玻璃与红石粉'),
      P('forest-restoration', '林地修复', '补种被采伐的树苗。', '林地修复', 'plant', '每 400 tick 在相邻泥土上种下一棵对应树苗且只在光照足够时执行', '树苗、骨粉与木板'),
      P('biome-balance', '群系平衡', '监测设施覆盖对自然区域的挤压。', '群系平衡', 'research', '当半径 16 格内机器超过 12 台时显示橙色建设密度提示', '地图、书与红石粉'),
      P('protection-grid', '保护网格', '阻止作业设施越过授权边界。', '保护网格', 'construction', '当机器目标位于边界外时取消作业并显示红色粒子', '铁锭、书与红石比较器'),
      P('impact-report', '影响报告', '把环境变化汇总为可读记录。', '环境报告', 'research', '每 1200 tick 统计燃烧、用水、采收和回收次数并写入报告', '书、羽毛笔与红石粉'),
    ],
  }),
  D({
    id: 'exploration', stage: 3, name: '探险地理',
    tagline: '用地图、补给、信标和救援协议降低未知区域的探索成本。',
    overview: '探险地理规划洞穴、遗迹、营地和跨群系行程，让玩家带着可回程的信息进入新区域。',
    learningGoals: ['在离开据点前准备地图、补给和信标。', '根据地形选择攀爬、洞穴和水面路线。', '在迷路或受伤时执行明确的救援路径。'],
    families: [
      P('survey-map', '区域地图', '按网格记录已经安全走过的区域。', '区域地图', 'research', '每移动 32 格在地图上写入一个当前维度的安全节点', '地图、纸与指南针'),
      P('compass-route', '罗盘路线', '把目标与当前位置连接为可见指引。', '罗盘路线', 'research', '显示绑定目标的方向箭头与距离，最大距离 2048 格', '指南针、红石粉与空间碎片'),
      P('climbing-kit', '攀爬装备', '为陡壁和竖井提供短距安全移动。', '攀爬装备', 'defense', '面对墙体使用后在 8 格内生成可攀爬点并持续 30 秒', '强力丝线、铁锭与木棍'),
      P('cave-nav', '洞穴导航', '记录地下岔路并防止循环迷路。', '洞穴导航', 'research', '每经过岔路显示最近两个已访问节点的方向与距离', '火把、书与红石粉'),
      P('ruin-scan', '遗迹扫描', '在不破坏方块前标记结构轮廓。', '遗迹扫描', 'research', '扫描半径 10 格内苔石砖并显示白色边缘 10 秒', '望远镜、青金石与红石粉'),
      P('field-camp', '野外营地', '在远离据点时提供临时补给节点。', '野外营地', 'construction', '放置后在半径 4 格内显示床位、箱子和火源的状态', '床、箱子与篝火'),
      P('supply-pack', '补给包', '将探索必需物品固定为可检查套装。', '补给包', 'logistics', '打开后按固定顺序提供食物、火把、方块和绳索各一组', '皮革、箱子与纸'),
      P('threat-sense', '风险感知', '对临近危险实体给出方向提示。', '探险风险', 'research', '在半径 12 格内出现敌对实体时显示红色方向箭头', '蜘蛛眼、红石粉与玻璃'),
      P('portal-finder', '门户定位', '定位已绑定传送门的方位。', '门户定位', 'space', '显示最近绑定门户的方向并每 10 秒刷新一次', '末影珍珠、指南针与空间碎片'),
      P('rescue-beacon', '救援信标', '在远距离请求援助时留下可找回位置。', '救援信标', 'defense', '激活后向授权玩家显示使用者坐标并持续 120 秒', '钟、铁锭与红石灯'),
    ],
  }),
  D({
    id: 'commerce', stage: 3, name: '聚落经济',
    tagline: '让交易、订单、信誉和公共物资在玩家之间保持可见与可审计。',
    overview: '聚落经济规划市场、合约、货运、工坊协作和公共建设，为多人服务器的物资流动建立透明规则。',
    learningGoals: ['为交易写清物品、数量、价格和到期条件。', '区分个人库存、工坊库存和公共储备。', '通过日志解决错放、缺货和未完成订单。'],
    families: [
      P('market-board', '市场公告', '公开展示可交易物品和数量。', '市场公告', 'commerce', '显示前 6 个上架物品的名称、数量和单价', '告示牌、书与红石粉'),
      P('vending', '自助售卖', '在固定价格下完成单件交易。', '自助售卖', 'commerce', '收到正确支付物后输出 1 个指定物品并记录交易次数', '箱子、漏斗与红石比较器'),
      P('work-contract', '工坊合约', '把加工任务拆成输入、输出和截止条件。', '工坊合约', 'commerce', '在输入齐全时创建任务并在输出完成后标记交付', '书、羽毛笔与电路板'),
      P('reputation', '信誉记录', '记录按时交付与取消订单的行为。', '信誉记录', 'research', '每完成一笔合约增加 1 点信誉并在取消时记录原因', '书、绿宝石与红石粉'),
      P('price-panel', '价格面板', '让常用物资价格有时间标记。', '价格面板', 'commerce', '显示指定物品过去 10 次交易的最低、最高和最新价格', '告示牌、红石粉与纸'),
      P('cargo-insurance', '货运保障', '在运输失败时保留货物归属。', '货运保障', 'commerce', '货物未到达绑定站点时将其送回原始容器并记录事件', '箱子、书与末影珍珠'),
      P('cooperative', '协作工坊', '为多名玩家分配明确作业权限。', '协作工坊', 'commerce', '仅允许授权成员启动工坊机器并显示当前操作成员', '书、铁锭与红石比较器'),
      P('quest-permit', '委托许可', '为开放式任务设置可验证目标。', '委托许可', 'commerce', '在玩家交付指定物品后自动完成任务并发放固定奖励', '纸、绿宝石与红石粉'),
      P('artisan-guild', '工匠行会', '按学科记录成员擅长工艺。', '工匠行会', 'research', '显示成员最近完成的三类学科任务与完成时间', '书、羽毛笔与金锭'),
      P('public-works', '公共工程', '把多人材料投入与工程进度公开化。', '公共工程', 'construction', '每提交 16 个指定材料将公共工程进度增加 1%，最大 100%', '告示牌、箱子与铁锭'),
    ],
  }),
  D({
    id: 'quantum', stage: 4, name: '量子工程',
    tagline: '用严格容量、配对和稳定性约束连接远距离信息与物品。',
    overview: '量子工程规划量子缓存、纠缠配对、退相干保护与受限传输，所有跨距效果均以绑定关系和容量为边界。',
    learningGoals: ['理解量子配对、缓存容量和稳定时间。', '在启用传输前检查两端授权和库存。', '让不稳定状态触发回退而不是丢失物品。'],
    families: [
      P('qubit', '量子位元', '将一个有限状态写入可验证载体。', '量子位元', 'quantum', '保存一个布尔状态并在读写后显示蓝色或白色粒子', '紫水晶、红石粉与玻璃'),
      P('entanglement', '纠缠配对', '让两端设备共享一条固定通道。', '纠缠配对', 'quantum', '将两个已授权设备配对并拒绝第三个设备加入通道', '末影珍珠、紫水晶与铁锭'),
      P('data-compression', '数据压缩', '将重复的作业记录压成短摘要。', '量子压缩', 'quantum', '把连续 64 条相同机器事件压缩为一条带数量的记录', '纸、紫水晶与红石粉'),
      P('quantum-storage', '量子存储', '以固定槽位保存跨区块的少量物品。', '量子存储', 'quantum', '保存前 9 格物品并在读取时校验物品名称与数量一致', '箱子、空间碎片与紫水晶'),
      P('decoder', '状态解码', '把量子缓存转为人类可读信息。', '量子解码', 'quantum', '显示绑定缓存的槽位占用、稳定时间和最后写入者', '书、玻璃与电路板'),
      P('quantum-schedule', '量子调度', '按单一顺序提交跨距任务。', '量子调度', 'quantum', '每 20 tick 只执行一个已配对传输任务并按序号递增', '时钟、紫水晶与电路板'),
      P('teleport-control', '传送控制', '阻止无目标或无授权的量子移动。', '量子传送', 'quantum', '当目标未绑定或玩家未授权时取消传送并保持原位', '末影珍珠、书与红石比较器'),
      P('decoherence', '退相干防护', '在能量或配对失效前锁定传输。', '退相干防护', 'quantum', '当稳定值低于 20% 时冻结新写入并显示紫色警告', '紫水晶块、铁锭与红石粉'),
      P('quantum-sensor', '量子传感', '观测远端设备的有限状态。', '量子传感', 'quantum', '每 40 tick 读取绑定设备的运行或停机状态且不读取库存内容', '玻璃、紫水晶与电路板'),
      P('quantum-fabrication', '量子装配', '只在完整输入齐全时提交一次装配。', '量子装配', 'quantum', '当 9 个输入槽全部匹配时消耗输入并生成一个确定输出', '铁锭、紫水晶与机器核心'),
    ],
  }),
  D({
    id: 'dimensional', stage: 4, name: '维度航行',
    tagline: '把下界、末地与主世界的移动限制在稳定坐标和回程协议内。',
    overview: '维度航行规划门框、锚点、相位稳定与边界换算，让跨维度体验以清楚的起终点和失败回退为前提。',
    learningGoals: ['为每次跨维度移动记录起点与回程点。', '识别维度资源与环境风险的区别。', '在坐标或框架失效时执行原地回退。'],
    families: [
      P('rift-chart', '裂隙图谱', '记录跨维度门户的对应关系。', '裂隙图谱', 'space', '显示已绑定门户的维度名、坐标与最后一次使用时间', '地图、空间碎片与书'),
      P('gateway-frame', '门框工程', '保证门户使用前具备完整结构。', '门框工程', 'space', '在门框缺角或被水覆盖时显示红色轮廓并拒绝开启', '黑曜石、铁锭与空间碎片'),
      P('dimensional-anchor', '维度锚定', '把一个站立安全点写入当前维度。', '维度锚定', 'space', '只在下方有两格实体方块时允许保存返回坐标', '铁块、空间碎片与指南针'),
      P('phase-stabilizer', '相位稳定', '限制传送过程中的目标漂移。', '相位稳定', 'space', '传送落点偏差超过 2 格时取消移动并返回起点', '紫水晶、黑曜石与红石粉'),
      P('ender-ecology', '末地生态', '观察末地环境中的可用生物资源。', 'research 末地生态', 'research', '记录半径 16 格内紫颂植物与末地石数量并持续 10 秒', '末影粉尘、书与玻璃'),
      P('nether-thermal', '下界热工', '把下界高温区域转为受限热源。', '下界热工', 'energy', '仅在下界维度每 20 tick 向相连设备提供 2 点热能', '火焰锭、铁锭与黑曜石'),
      P('border-transit', '边界通行', '在维度边界外阻止设施继续工作。', '边界通行', 'space', '当目标坐标超过世界边界时取消移动并显示黄色粒子', '指南针、红石比较器与空间碎片'),
      P('coordinate-converter', '坐标换算', '显示两种维度坐标的对应位置。', '坐标换算', 'research', '将主世界坐标按 8 比 1 显示为下界建议坐标且不自动移动', '纸、指南针与红石粉'),
      P('foreign-material', '异界材料', '为跨维度材料建立来源标记。', '异界材料', 'resource', '将来源维度写入材料标签并在错误维度使用时显示提示', '末影粉尘、火焰锭与石英'),
      P('return-protocol', '回程协议', '在探险结束时执行明确的回程确认。', 'space 回程', 'space', '使用前显示目标维度和坐标，确认后才开始传送', '书、空间碎片与钟'),
    ],
  }),
  D({
    id: 'chronology', stage: 4, name: '时间学',
    tagline: '以日历、缓冲与时序记录协调作物、机器和天气，而不修改世界时间。',
    overview: '时间学只管理可观察的计划和冷却，不改变服务器世界时间；它将生长、维护、天气和任务安排写为可读时序。',
    learningGoals: ['区分世界时间与设备任务时间。', '为长期作业设置可见进度和截止条件。', '在时间异常时保留状态而不是重复执行。'],
    families: [
      P('time-shard', '时序碎片', '记录一次事件的精确 tick。', '时序碎片', 'research', '写入最近一次机器启动的世界 tick 并显示其经过时间', '石英、红石粉与玻璃'),
      P('chrono-clock', '时序时钟', '以固定周期驱动不重叠任务。', '时序时钟', 'machine', '每 200 tick 触发一次任务且上一次未完成时跳过本次', '时钟、红石比较器与电路板'),
      P('aging-monitor', '成熟监测', '显示作物和设备距离下一状态的时间。', '成熟监测', 'research', '右键作物或机器后显示其下一次状态变化的剩余 tick', '书、时钟与红石粉'),
      P('weather-timeline', '天气时线', '记录天气事件的开始与持续时间。', '天气时线', 'research', '在降雨或雷暴结束时写入开始 tick和持续 tick', '纸、时钟与玻璃'),
      P('crop-timing', '作物时机', '避免在未成熟时重复采收。', '作物时机', 'plant', '仅在作物成熟时允许自动采收，未成熟时保持种子不动', '种子、时钟与电路板'),
      P('task-buffer', '任务缓冲', '在区块卸载前保存正在进行的作业。', '任务缓冲', 'machine', '区块卸载时保存任务剩余 tick，加载后从该值继续', '书、红石粉与铁锭'),
      P('phase-calendar', '阶段日历', '为多人活动显示统一进度阶段。', '阶段日历', 'research', '显示当前活动的开始时间、结束时间和完成百分比', '书、时钟与金锭'),
      P('temporal-anchor', '时序锚点', '阻止同一任务在短时间内重复创建。', '时序锚点', 'machine', '同一任务键在 40 tick 内再次请求时拒绝创建并返回已有序号', '铁锭、时钟与电路板'),
      P('maintenance-window', '维护窗口', '为机器设定不接收新作业的时间段。', '维护窗口', 'machine', '到达设定 tick 区间时停止新任务并允许已开始任务完成', '时钟、红石中继器与书'),
      P('history-archive', '时序档案', '把关键状态变化保存为可查记录。', '时序档案', 'research', '保存最近 128 条状态变化的时间、位置和设备名称', '书、羽毛笔与红石粉'),
    ],
  }),
  D({
    id: 'astral', stage: 4, name: '星辉学',
    tagline: '以日月、星图和观测台把天空变化转为导航和计划信息。',
    overview: '星辉学规划观星、月相、日照和陨石观测，服务于夜间探索、光能与远距定位。',
    learningGoals: ['识别昼夜、月相与天气对观测的影响。', '用星图而不是随机标记选择探索方向。', '在天空被遮挡时接受观测失败反馈。'],
    families: [
      P('starlight', '星光采集', '在晴朗夜空下记录星辉强度。', '星光采集', 'energy', '在夜晚且天空可见时每 20 tick 产生 1 点星辉能量', '玻璃、紫水晶与铁锭'),
      P('constellation', '星座识别', '把星图位置映射为可见方向。', '星座识别', 'research', '在夜晚显示最近已记录星座的方位箭头并持续 10 秒', '纸、望远镜与紫水晶'),
      P('lunar-cycle', '月相周期', '读取月亮状态影响的活动窗口。', '月相周期', 'research', '显示当前月相名称和距离下一月相的剩余游戏日', '时钟、纸与玻璃'),
      P('solar-chart', '日照图表', '识别建筑顶部的有效日照时段。', '日照图表', 'research', '每 200 tick 记录当前位置天空光照并绘制最近 6 次读数', '书、玻璃与红石粉'),
      P('meteor-watch', '流星观测', '为高空落物提供不自动生成战利品的预警。', '流星观测', 'research', '在天空中出现火焰粒子事件时显示其方位并持续 15 秒', '望远镜、火焰锭与红石粉'),
      P('astral-navigation', '星际导航', '利用星图为远行提供稳定方向。', '星际导航', 'research', '在夜晚将罗盘指向选定星座方向且不修改出生点', '指南针、紫水晶与纸'),
      P('telescope', '天文望远', '放大观察远处天空与高处结构。', '天文望远', 'research', '右键后将视野锁定于前方 64 格范围并显示目标方块名称', '玻璃、铜锭与紫水晶'),
      P('astral-beacon', '星辉信标', '在夜晚标识可安全抵达的据点。', '星辉信标', 'construction', '夜晚在半径 64 格内显示白色垂直光束且不提供增益', '玻璃、紫水晶与铁块'),
      P('star-map', '星图档案', '保存玩家完成的观测记录。', '星图档案', 'research', '记录已观测星座、观测坐标和天气条件', '书、纸与紫水晶'),
      P('observatory', '观星台', '为连续天空观测提供固定设施。', '观星台', 'construction', '放置在天空可见处后每 200 tick 更新一次星辉读数', '玻璃、石英与铁锭'),
    ],
  }),
  D({
    id: 'ender', stage: 4, name: '末影学',
    tagline: '从末地石、紫颂与虚空风险中提炼可控的末影工艺。',
    overview: '末影学规划末地材料、潜影、龙息与虚空防护，所有效果都必须限定在末地环境与可验证目标中。',
    learningGoals: ['区分末地资源、传送素材和虚空风险。', '在末地作业前准备防坠和回程措施。', '让末影设备显示目标和范围而不是隐式移动。'],
    families: [
      P('endstone', '末地岩材', '把末地石转化为可加工结构输入。', '末地岩材', 'resource', '将末地石粉碎为可用于末影配方的稳定颗粒', '末地石、铁锭与石英'),
      P('chorus', '紫颂培育', '在受控范围内培育紫颂植物。', '紫颂培育', 'plant', '每 600 tick 在末地石上生成一株紫颂花且周围 2 格为空气', '紫颂果、末地石与水瓶'),
      P('shulker', '潜影收纳', '以有限槽位整理探索物资。', '潜影收纳', 'logistics', '只允许存入前 9 格物品并在满槽时显示紫色粒子', '潜影壳、箱子与末影珍珠'),
      P('dragon-breath', '龙息采样', '把龙息区域标为不可驻留的短时风险。', '龙息采样', 'research', '在龙息云附近 4 格显示紫色边缘并持续 6 秒', '玻璃瓶、末影粉尘与铁锭'),
      P('void-guard', '虚空防护', '在末地边缘提前提醒坠落风险。', '虚空防护', 'defense', '当玩家下方 16 格无实体方块时显示红色粒子和方向箭头', '羽毛、空间碎片与铁锭'),
      P('ender-pearl', '末影投掷', '限制末影珍珠移动的有效落点。', '末影投掷', 'space', '当投掷目标下方无安全站立方块时取消落点并将珍珠返还', '末影珍珠、铁锭与玻璃'),
      P('gateway', '末地通道', '校验通道两端的授权和安全站立面。', '末地通道', 'space', '仅在两端都有已授权锚点时允许开启 30 秒通道', '黑曜石、末影粉尘与空间碎片'),
      P('void-resonance', '虚空共振', '观测末地岩层的稳定频率。', '虚空共振', 'research', '每 40 tick 显示最近末地石团块的方向且最大距离 24 格', '紫水晶、末地石与红石粉'),
      P('obsidian-tools', '黑曜石工具', '为硬质方块作业设定耐久边界。', '黑曜石工具', 'machine', '每次作业只处理黑曜石类白名单方块并消耗 2 点耐久', '黑曜石、铁锭与火焰锭'),
      P('end-lab', '末地实验台', '让末地配方限定在对应维度执行。', '末地实验台', 'machine', '当不在末地维度时拒绝开始作业并显示紫色提示', '末地石、铁锭与电路板'),
    ],
  }),
  D({
    id: 'civic', stage: 4, name: '城邦工程',
    tagline: '将多人聚落的权限、公共服务与应急响应编排为可理解规则。',
    overview: '城邦工程规划区域、角色、公共工程、能源章程与灾害响应，确保多人服务器的公共设施可审计而不越权。',
    learningGoals: ['区分个人、工坊和公共区域权限。', '用公开进度管理公共建设资源。', '在异常事件中优先保护玩家与可恢复数据。'],
    families: [
      P('district-map', '区域划分', '用边界定义聚落中的功能区。', '区域划分', 'construction', '显示半径 16 格内居住、生产或公共区域的名称与边界', '地图、书与铁锭'),
      P('role-protocol', '角色协议', '将可执行操作分配给明确角色。', '角色协议', 'commerce', '仅允许拥有对应角色的玩家启动指定公共设备', '书、羽毛笔与红石比较器'),
      P('public-inventory', '公共库存', '将公共物资与个人物资分开记录。', '公共库存', 'logistics', '每次取放时记录玩家、物品、数量和容器坐标', '箱子、书与红石粉'),
      P('works-board', '工程看板', '公开展示公共工程的材料与进度。', '工程看板', 'commerce', '显示工程所需材料、已提交数量和完成百分比', '告示牌、箱子与纸'),
      P('defense-alert', '防御警报', '在聚落受到入侵时通知授权成员。', '防御警报', 'defense', '未授权玩家进入边界时显示坐标并播放一次钟声', '钟、红石灯与铁锭'),
      P('knowledge-archive', '知识档案', '保存可共享的工艺与维护说明。', '知识档案', 'research', '显示最近 32 条公开工艺记录的标题、作者和更新时间', '书、书架与红石粉'),
      P('transit-authority', '通行管理', '约束公共物流和传送节点的使用范围。', '通行管理', 'logistics', '当玩家未授权时拒绝使用绑定中继站或传送锚点', '铁锭、书与空间碎片'),
      P('energy-charter', '能源章程', '为公共电网设定优先级与保护阈值。', '能源章程', 'energy', '当公共储能低于 20% 时保留照明和救援设备的供电', '书、电路板与红石比较器'),
      P('disaster-response', '应急响应', '为火灾、洪水和系统故障建立告警流程。', '应急响应', 'defense', '检测到绑定设施停机时显示坐标并发送一次公共警报', '钟、铁锭与红石灯'),
      P('graduation-station', '学科考核', '根据完成的学习目标开放下一阶段入口。', '学科考核', 'research', '统计当前学科已完成目标数并在达到 3 项时显示绿色提示', '书、经验瓶与铁锭'),
    ],
  }),
]);

const DISCIPLINES = Object.freeze(BLUEPRINTS.map(assembleDiscipline));
const ALL_ITEMS = Object.freeze(DISCIPLINES.flatMap((discipline) => discipline.items));
const CATALOG_STATS = Object.freeze({
  disciplineCount: DISCIPLINES.length,
  itemCount: ALL_ITEMS.length,
  familyCount: DISCIPLINES.reduce((count, discipline) => count + discipline.families.length, 0),
  waveCount: CAMPAIGN_WAVES.length,
  implementedCount: ALL_ITEMS.filter((item) => item.status === STATUS.implemented).length,
  plannedCount: ALL_ITEMS.filter((item) => item.status === STATUS.planned).length,
  narrativeAnchorFamilyCount: DISCIPLINES.reduce((count, discipline) => count + discipline.families.filter((family) => family.isNarrativeAnchor).length, 0),
  storyCount: ALL_ITEMS.filter((item) => item.story !== null).length,
  familyLinkCount: CAMPAIGN_WAVES.reduce((count, wave) => count + wave.familyLinks.length, 0),
});

function assertCatalog(condition, message) {
  if (!condition) {
    throw new Error(`TalexSoulTech catalog invariant failed: ${message}`);
  }
}

function validateCatalog(disciplines, stats) {
  const expectedWaveIds = Array.from({ length: 9 }, (_, index) => `W${index + 1}`);
  assertCatalog(Array.isArray(CAMPAIGN_WAVES), 'campaign waves must be an array');
  assertCatalog(CAMPAIGN_WAVES.length === 9, `expected 9 campaign waves, received ${CAMPAIGN_WAVES.length}`);
  const waveById = new Map();
  const progressionDisciplineIds = new Set();
  const anchorFamilyKeys = new Set();
  const storyIds = new Set();
  const familyLinkKeys = new Set();
  let familyLinkCount = 0;
  for (const [index, wave] of CAMPAIGN_WAVES.entries()) {
    assertCatalog(wave.id === expectedWaveIds[index], `campaign wave order must be W1-W9, received ${wave.id}`);
    assertCatalog(wave.order === index + 1, `${wave.id} has invalid order`);
    assertCatalog(!waveById.has(wave.id), `duplicate campaign wave ${wave.id}`);
    waveById.set(wave.id, wave);
    assertCatalog(wave.disciplineIds.length === 3, `${wave.id} must contain exactly 3 disciplines`);
    assertCatalog(wave.disciplineArcs.length === 3, `${wave.id} must contain exactly 3 discipline arcs`);
    assertCatalog(wave.anchors.length === 6, `${wave.id} must contain exactly 6 anchors`);
    const waveArcIds = new Set();
    for (const arc of wave.disciplineArcs) {
      assertCatalog(!waveArcIds.has(arc.id), `${wave.id} has duplicate discipline arc ${arc.id}`);
      assertCatalog(wave.disciplineIds.includes(arc.id), `${wave.id} arc ${arc.id} is not listed by disciplineIds`);
      waveArcIds.add(arc.id);
      progressionDisciplineIds.add(arc.id);
      for (const key of ['role', 'whyNow', 'input', 'output', 'recovery']) {
        assertCatalog(typeof arc[key] === 'string' && arc[key].trim().length > 0, `${arc.id} has an invalid progression ${key}`);
      }
    }
    for (const anchor of wave.anchors) {
      const expectedFamilyKey = `${anchor.disciplineId}.${anchor.familyId}`;
      assertCatalog(wave.disciplineIds.includes(anchor.disciplineId), `${wave.id} anchor ${anchor.familyKey} points outside its wave`);
      assertCatalog(anchor.familyKey === expectedFamilyKey, `${anchor.familyKey} is not a stable discipline.family key`);
      assertCatalog(!anchorFamilyKeys.has(anchor.familyKey), `duplicate narrative anchor ${anchor.familyKey}`);
      assertCatalog(anchor.stories.length === 3, `${anchor.familyKey} must contain exactly 3 stories`);
      assertCatalog(typeof anchor.reason === 'string' && anchor.reason.trim().length > 0, `${anchor.familyKey} has no anchor reason`);
      anchorFamilyKeys.add(anchor.familyKey);
      const orders = anchor.stories.map((story) => story.order).sort((left, right) => left - right);
      assertCatalog(orders.join(',') === '1,2,3', `${anchor.familyKey} story order must be 1,2,3`);
      for (const story of anchor.stories) {
        assertCatalog(!storyIds.has(story.itemId), `duplicate narrative story ${story.itemId}`);
        assertCatalog(typeof story.text === 'string' && story.text.trim().length > 0, `${story.itemId} has empty story text`);
        assertCatalog(!story.text.includes('Tier'), `${story.itemId} story text must not contain Tier metadata`);
        assertCatalog(!story.text.includes(story.itemId), `${story.itemId} story text must not repeat its item ID`);
        storyIds.add(story.itemId);
      }
    }
    for (const link of wave.familyLinks) {
      assertCatalog(link.kind === 'supports', `${wave.id} has unsupported family link kind ${link.kind}`);
      assertCatalog(typeof link.from === 'string' && typeof link.to === 'string', `${wave.id} has an invalid family link endpoint`);
      assertCatalog(link.from !== link.to, `${wave.id} has a self-referencing family link ${link.from}`);
      const familyLinkKey = `${link.kind}:${link.from}->${link.to}`;
      assertCatalog(!familyLinkKeys.has(familyLinkKey), `${wave.id} has duplicate family link ${familyLinkKey}`);
      familyLinkKeys.add(familyLinkKey);
      assertCatalog(typeof link.reason === 'string' && link.reason.trim().length > 0, `${wave.id} has a family link without a reason`);
      familyLinkCount += 1;
    }
  }
  assertCatalog(progressionDisciplineIds.size === 27, `expected 27 progression discipline arcs, received ${progressionDisciplineIds.size}`);
  assertCatalog(anchorFamilyKeys.size === 54, `expected 54 unique narrative anchor families, received ${anchorFamilyKeys.size}`);
  assertCatalog(storyIds.size === 162, `expected 162 unique narrative stories, received ${storyIds.size}`);
  assertCatalog(familyLinkCount === 96, `expected 96 unique soft family links, received ${familyLinkCount}`);

  assertCatalog(disciplines.length === 27, `expected 27 disciplines, received ${disciplines.length}`);
  const disciplineIds = new Set();
  const itemIds = new Set();
  const familyByKey = new Map();
  const catalogItemsByFamily = new Map();
  let itemCount = 0;
  let implementedCount = 0;
  let plannedCount = 0;
  let narrativeFamilyCount = 0;
  let narrativeStoryCount = 0;

  for (const discipline of disciplines) {
    assertCatalog(!disciplineIds.has(discipline.id), `duplicate discipline id ${discipline.id}`);
    disciplineIds.add(discipline.id);
    assertCatalog(waveById.has(discipline.waveId), `${discipline.id} points at an unknown wave ${discipline.waveId}`);
    assertCatalog(waveById.get(discipline.waveId).disciplineIds.includes(discipline.id), `${discipline.id} is not listed in ${discipline.waveId}`);
    assertCatalog(discipline.progression && typeof discipline.progression === 'object', `${discipline.id} has no progression arc`);
    for (const key of ['role', 'whyNow', 'input', 'output', 'recovery']) {
      assertCatalog(typeof discipline.progression[key] === 'string' && discipline.progression[key].trim().length > 0, `${discipline.id} has an invalid progression ${key}`);
    }
    assertCatalog(Number.isInteger(discipline.stage) && discipline.stage >= 1 && discipline.stage <= 4, `invalid stage for ${discipline.id}`);
    assertCatalog(discipline.learningGoals.length === 3, `${discipline.id} must contain exactly 3 learning goals`);
    assertCatalog(discipline.families.length === 10, `${discipline.id} must contain exactly 10 family concepts`);
    assertCatalog(discipline.items.length === 30, `${discipline.id} must contain exactly 30 items`);

    const familyNames = new Set();
    const familyIds = new Set();
    for (const family of discipline.families) {
      const expectedFamilyKey = `${discipline.id}.${family.id}`;
      assertCatalog(typeof family.id === 'string' && family.id.length > 0, `${discipline.id} has an invalid family id`);
      assertCatalog(!familyIds.has(family.id), `${discipline.id} has duplicate family id ${family.id}`);
      assertCatalog(typeof family.name === 'string' && family.name.length > 0, `${discipline.id} has an invalid family name`);
      assertCatalog(typeof family.concept === 'string' && family.concept.length > 0, `${discipline.id} has an invalid family concept`);
      assertCatalog(!familyNames.has(family.name), `${discipline.id} has duplicate family ${family.name}`);
      assertCatalog(family.key === expectedFamilyKey, `${discipline.id}.${family.id} has an invalid family key`);
      assertCatalog(family.isNarrativeAnchor === anchorFamilyKeys.has(family.key), `${family.key} narrative anchor flag drifted`);
      assertCatalog(family.anchorReason === null || (typeof family.anchorReason === 'string' && family.anchorReason.trim().length > 0), `${family.key} has an invalid anchor reason`);
      familyNames.add(family.name);
      familyIds.add(family.id);
      familyByKey.set(family.key, family);
      if (family.isNarrativeAnchor) narrativeFamilyCount += 1;
    }

    for (const [declarationIndex, item] of discipline.items.entries()) {
      assertCatalog(typeof item.id === 'string' && item.id.length > 0, `${discipline.id} has an invalid item id`);
      assertCatalog(!itemIds.has(item.id), `duplicate item id ${item.id}`);
      itemIds.add(item.id);
      assertCatalog(item.disciplineId === discipline.id, `${item.id} belongs to the wrong discipline`);
      assertCatalog(familyIds.has(item.familyId), `${item.id} points at an unknown family id`);
      assertCatalog(item.familyKey === `${discipline.id}.${item.familyId}`, `${item.id} has an invalid family key`);
      assertCatalog(item.waveId === discipline.waveId, `${item.id} has a mismatched wave`);
      assertCatalog(familyNames.has(item.family), `${item.id} points at an unknown family name`);
      for (const key of ['name', 'tier', 'type', 'purpose', 'recipeHint']) {
        assertCatalog(typeof item[key] === 'string' && item[key].trim().length > 0, `${item.id} has an invalid ${key}`);
      }
      assertCatalog(Object.prototype.hasOwnProperty.call(TIER_RANK, item.tier), `${item.id} has an unknown Roman tier ${item.tier}`);
      assertCatalog(item.status === STATUS.implemented || item.status === STATUS.planned, `${item.id} has invalid status ${item.status}`);
      assertCatalog(itemIds.has(item.id), `${item.id} was not registered`);
      const familyItems = catalogItemsByFamily.get(item.familyKey) ?? [];
      familyItems.push({ item, declarationIndex });
      catalogItemsByFamily.set(item.familyKey, familyItems);
      const expectedStory = PROGRESSION_INDEX.storyByItemId.get(item.id);
      assertCatalog(item.isNarrativeAnchor === Boolean(expectedStory), `${item.id} narrative anchor flag drifted`);
      if (expectedStory) {
        assertCatalog(item.story && item.story.order === expectedStory.order, `${item.id} story order drifted`);
        assertCatalog(item.story.text === expectedStory.text, `${item.id} story text drifted`);
        assertCatalog(item.story.anchorReason === expectedStory.anchorReason, `${item.id} story anchor reason drifted`);
        narrativeStoryCount += 1;
      } else {
        assertCatalog(item.story === null, `${item.id} unexpectedly has a story`);
      }
      itemCount += 1;
      if (item.status === STATUS.implemented) implementedCount += 1;
      if (item.status === STATUS.planned) plannedCount += 1;
    }
  }

  assertCatalog(disciplineIds.size === 27, `expected 27 catalog disciplines, received ${disciplineIds.size}`);
  assertCatalog(disciplineIds.size === progressionDisciplineIds.size, 'catalog/progression discipline coverage differs');
  assertCatalog(familyByKey.size === 270, `expected 270 catalog families, received ${familyByKey.size}`);
  assertCatalog(itemCount === 810, `expected 810 items, received ${itemCount}`);
  assertCatalog(implementedCount + plannedCount === 810, 'status totals must equal 810');
  assertCatalog(narrativeFamilyCount === 54, `expected 54 narrative families, received ${narrativeFamilyCount}`);
  assertCatalog(narrativeStoryCount === 162, `expected 162 narrative stories, received ${narrativeStoryCount}`);

  for (const [familyKey, entries] of catalogItemsByFamily.entries()) {
    const ordered = [...entries].sort((left, right) => (
      TIER_RANK[left.item.tier] - TIER_RANK[right.item.tier]
      || left.declarationIndex - right.declarationIndex
    ));
    ordered.forEach(({ item }, index) => {
      assertCatalog(item.previousItemId === (ordered[index - 1]?.item.id ?? null), `${item.id} has an invalid previous item suggestion`);
      assertCatalog(item.nextItemId === (ordered[index + 1]?.item.id ?? null), `${item.id} has an invalid next item suggestion`);
      if (item.previousItemId) assertCatalog(itemById(itemIds, disciplines, item.previousItemId).familyKey === familyKey, `${item.id} previous item leaves its family`);
      if (item.nextItemId) assertCatalog(itemById(itemIds, disciplines, item.nextItemId).familyKey === familyKey, `${item.id} next item leaves its family`);
    });
  }

  const catalogItemById = new Map(disciplines.flatMap((discipline) => discipline.items.map((item) => [item.id, item])));
  for (const wave of CAMPAIGN_WAVES) {
    for (const anchor of wave.anchors) {
      const family = familyByKey.get(anchor.familyKey);
      assertCatalog(family && family.isNarrativeAnchor, `${anchor.familyKey} anchor family is missing from catalog`);
      for (const story of anchor.stories) {
        const item = catalogItemById.get(story.itemId);
        assertCatalog(item && item.familyKey === anchor.familyKey, `${story.itemId} story points at the wrong family`);
        assertCatalog(item.story !== null, `${story.itemId} anchor story is missing from catalog`);
      }
    }
    for (const link of wave.familyLinks) {
      assertCatalog(familyByKey.has(link.from), `${wave.id} link source ${link.from} is unknown`);
      assertCatalog(familyByKey.has(link.to), `${wave.id} link target ${link.to} is unknown`);
    }
  }

  assertCatalog(stats.disciplineCount === 27, `stats disciplineCount must be 27, received ${stats.disciplineCount}`);
  assertCatalog(stats.itemCount === 810, `stats itemCount must be 810, received ${stats.itemCount}`);
  assertCatalog(stats.familyCount === 270, `stats familyCount must be 270, received ${stats.familyCount}`);
  assertCatalog(stats.waveCount === 9, `stats waveCount must be 9, received ${stats.waveCount}`);
  assertCatalog(stats.implementedCount === implementedCount, 'stats implementedCount does not match item data');
  assertCatalog(stats.plannedCount === plannedCount, 'stats plannedCount does not match item data');
  assertCatalog(stats.narrativeAnchorFamilyCount === 54, 'stats narrativeAnchorFamilyCount must be 54');
  assertCatalog(stats.storyCount === 162, 'stats storyCount must be 162');
  assertCatalog(stats.familyLinkCount === 96, 'stats familyLinkCount must be 96');
}

function itemById(itemIds, disciplines, id) {
  if (!itemIds.has(id)) throw new Error(`missing catalog item ${id}`);
  for (const discipline of disciplines) {
    const item = discipline.items.find((candidate) => candidate.id === id);
    if (item) return item;
  }
  throw new Error(`missing catalog item ${id}`);
}


validateCatalog(DISCIPLINES, CATALOG_STATS);

export { DISCIPLINES, CATALOG_STATS };
