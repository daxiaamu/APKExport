(() => {
  const visual = document.querySelector('.package');
  const icon = visual?.querySelector('img');
  const tags = [...(visual?.querySelectorAll('.package-format') || [])];
  const cords = [...(visual?.querySelectorAll('.package-cords path') || [])];
  if (!visual || !icon || tags.length !== 4 || cords.length !== 4) return;

  const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const finePointer = matchMedia('(pointer: fine)').matches;
  if (reduced || !finePointer) return;

  const decorations = [...visual.querySelectorAll('[data-decor-depth]')];
  const pointer = { x: 0, y: 0 };
  const iconState = { x: 0, y: 0, rx: 0, ry: 0 };
  const states = tags.map((tag, index) => ({
    tag,
    mass: Number(tag.dataset.mass || 1),
    x: 0,
    y: 0,
    vx: 0,
    vy: 0,
    angle: 0,
    angularVelocity: 0,
    index
  }));

  let frame = 0;
  let lastTime = 0;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const setVisual = (name, value) => visual.style.setProperty(name, value);

  function drawCords() {
    const box = visual.getBoundingClientRect();
    const iconBox = icon.getBoundingClientRect();
    const width = Math.max(1, box.width);
    const height = Math.max(1, box.height);
    const anchors = [
      { x: iconBox.left - box.left + 22, y: iconBox.top - box.top + iconBox.height * .3 },
      { x: iconBox.right - box.left - 22, y: iconBox.top - box.top + iconBox.height * .34 },
      { x: iconBox.left - box.left + 25, y: iconBox.bottom - box.top - iconBox.height * .27 },
      { x: iconBox.right - box.left - 25, y: iconBox.bottom - box.top - iconBox.height * .25 }
    ];

    visual.querySelector('.package-cords').setAttribute('viewBox', `0 0 ${width} ${height}`);
    states.forEach((state, index) => {
      const hole = state.tag.querySelector('.tag-hole').getBoundingClientRect();
      const end = {
        x: hole.left - box.left + hole.width / 2,
        y: hole.top - box.top + hole.height / 2
      };
      const start = anchors[index];
      const direction = end.x < start.x ? -1 : 1;
      const first = {
        x: start.x + direction * 30,
        y: start.y + (end.y - start.y) * .2
      };
      const second = {
        x: end.x - direction * (20 + Math.min(14, Math.abs(state.vx) * 2.5)),
        y: end.y - state.vy * 4
      };
      cords[index].setAttribute('d', `M ${start.x.toFixed(1)} ${start.y.toFixed(1)} C ${first.x.toFixed(1)} ${first.y.toFixed(1)}, ${second.x.toFixed(1)} ${second.y.toFixed(1)}, ${end.x.toFixed(1)} ${end.y.toFixed(1)}`);
    });
  }

  function requestTick() {
    if (!frame) frame = requestAnimationFrame(tick);
  }

  function tick(now) {
    frame = 0;
    const dt = lastTime ? Math.min(2, (now - lastTime) / 16.667) : 1;
    lastTime = now;

    const iconTargets = {
      x: pointer.x * 9,
      y: pointer.y * 7,
      rx: -pointer.y * 5,
      ry: pointer.x * 6
    };
    iconState.x += (iconTargets.x - iconState.x) * .2 * dt;
    iconState.y += (iconTargets.y - iconState.y) * .2 * dt;
    iconState.rx += (iconTargets.rx - iconState.rx) * .2 * dt;
    iconState.ry += (iconTargets.ry - iconState.ry) * .2 * dt;
    setVisual('--icon-x', `${iconState.x}px`);
    setVisual('--icon-y', `${iconState.y}px`);
    setVisual('--icon-rx', `${iconState.rx}deg`);
    setVisual('--icon-ry', `${iconState.ry}deg`);

    decorations.forEach(item => {
      const depth = Number(item.dataset.decorDepth || 1);
      item.style.setProperty('--decor-x', `${iconState.x * .5 * depth}px`);
      item.style.setProperty('--decor-y', `${iconState.y * .55 * depth}px`);
      item.style.setProperty('--decor-r', `${iconState.ry * .08 * depth}deg`);
    });

    let energy = Math.abs(iconTargets.x - iconState.x) + Math.abs(iconTargets.y - iconState.y);
    states.forEach(state => {
      const targetX = pointer.x * (6.8 + state.index * .35);
      const targetY = pointer.y * (5.2 + state.index * .25);
      const spring = .072 / state.mass;
      const damping = Math.pow(.82 + state.mass * .035, dt);
      state.vx = (state.vx + (targetX - state.x) * spring * dt) * damping;
      state.vy = (state.vy + (targetY - state.y) * spring * dt) * damping;
      state.x += state.vx * dt;
      state.y += state.vy * dt;

      const angleTarget = clamp(state.vx * 1.9 + pointer.x * .5, -8, 8);
      const angularSpring = .055 / state.mass;
      const angularDamping = Math.pow(.83 + state.mass * .03, dt);
      state.angularVelocity = (state.angularVelocity + (angleTarget - state.angle) * angularSpring * dt) * angularDamping;
      state.angle += state.angularVelocity * dt;

      state.tag.style.setProperty('--tag-x', `${state.x}px`);
      state.tag.style.setProperty('--tag-y', `${state.y}px`);
      state.tag.style.setProperty('--tag-r', `${state.angle}deg`);
      energy += Math.abs(targetX - state.x) + Math.abs(targetY - state.y) + Math.abs(state.vx) + Math.abs(state.vy) + Math.abs(state.angularVelocity);
    });

    drawCords();
    if (energy > .035) requestTick();
    else lastTime = 0;
  }

  visual.addEventListener('pointermove', event => {
    const box = visual.getBoundingClientRect();
    pointer.x = clamp(((event.clientX - box.left) / box.width - .5) * 2, -1, 1);
    pointer.y = clamp(((event.clientY - box.top) / box.height - .5) * 2, -1, 1);
    requestTick();
  });

  visual.addEventListener('pointerleave', () => {
    pointer.x = 0;
    pointer.y = 0;
    requestTick();
  });

  addEventListener('resize', drawCords, { passive: true });
  requestAnimationFrame(drawCords);
})();
