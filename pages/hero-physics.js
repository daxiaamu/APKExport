(() => {
  const visual = document.querySelector('.package');
  const icon = visual?.querySelector('img');
  const tags = [...(visual?.querySelectorAll('.package-format') || [])];
  const cord = visual?.querySelector('.package-cords path');
  if (!visual || !icon || tags.length !== 4 || !cord) return;

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

  function drawCord() {
    const box = visual.getBoundingClientRect();
    const width = Math.max(1, box.width);
    const height = Math.max(1, box.height);
    const order = [0, 1, 3, 2];
    const points = order.map(index => {
      const hole = states[index].tag.querySelector('.tag-hole').getBoundingClientRect();
      return {
        x: hole.left - box.left + hole.width / 2,
        y: hole.top - box.top + hole.height / 2,
        state: states[index]
      };
    });

    visual.querySelector('.package-cords').setAttribute('viewBox', `0 0 ${width} ${height}`);
    let path = `M ${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
    for (let index = 0; index < points.length; index++) {
      const previous = points[(index - 1 + points.length) % points.length];
      const current = points[index];
      const next = points[(index + 1) % points.length];
      const after = points[(index + 2) % points.length];
      const first = {
        x: current.x + (next.x - previous.x) / 6 + current.state.vx * .7,
        y: current.y + (next.y - previous.y) / 6 + current.state.vy * .7
      };
      const second = {
        x: next.x - (after.x - current.x) / 6 - next.state.vx * .7,
        y: next.y - (after.y - current.y) / 6 - next.state.vy * .7
      };
      path += ` C ${first.x.toFixed(1)} ${first.y.toFixed(1)}, ${second.x.toFixed(1)} ${second.y.toFixed(1)}, ${next.x.toFixed(1)} ${next.y.toFixed(1)}`;
    }
    cord.setAttribute('d', `${path} Z`);
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
    });

    let energy = Math.abs(iconTargets.x - iconState.x) + Math.abs(iconTargets.y - iconState.y);
    states.forEach(state => {
      const targetX = iconState.x * .78;
      const targetY = iconState.y * .72;
      const spring = .072 / state.mass;
      const damping = Math.pow(.82 + state.mass * .035, dt);
      state.vx = (state.vx + (targetX - state.x) * spring * dt) * damping;
      state.vy = (state.vy + (targetY - state.y) * spring * dt) * damping;
      state.x += state.vx * dt;
      state.y += state.vy * dt;

      const angleTarget = clamp(state.vx * 1.9 + pointer.x * .45, -8, 8);
      const angularSpring = .055 / state.mass;
      const angularDamping = Math.pow(.83 + state.mass * .03, dt);
      state.angularVelocity = (state.angularVelocity + (angleTarget - state.angle) * angularSpring * dt) * angularDamping;
      state.angle += state.angularVelocity * dt;

      state.tag.style.setProperty('--tag-x', `${state.x}px`);
      state.tag.style.setProperty('--tag-y', `${state.y}px`);
      state.tag.style.setProperty('--tag-r', `${state.angle}deg`);
      energy += Math.abs(targetX - state.x) + Math.abs(targetY - state.y) + Math.abs(state.vx) + Math.abs(state.vy) + Math.abs(state.angularVelocity);
    });

    drawCord();
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

  addEventListener('resize', drawCord, { passive: true });
  requestAnimationFrame(drawCord);
})();
