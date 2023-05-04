import { useFrame, useLoader } from "@react-three/fiber";
import { useEffect, useState } from "react";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader";
import * as THREE from "three";

type propsData = {
  repomonUrl: string;
};

export const Model1 = (props: propsData) => {
  const gltf = useLoader(GLTFLoader, props.repomonUrl);
  const [isClicked, setIsClicked] = useState<boolean>(false);

  let mixer: THREE.AnimationMixer | undefined;

  if (gltf.animations.length) {
    mixer = new THREE.AnimationMixer(gltf.scene);
    mixer.timeScale = 0.4;
    console.log("animations: ", gltf.animations);
    if (isClicked) {
      const action = mixer.clipAction(gltf.animations[11]);
      action.clampWhenFinished = true;
      action.play();
      setTimeout(() => {
        setIsClicked(false);
      }, 2000);
    } else {
      const action = mixer.clipAction(gltf.animations[8]);
      action.clampWhenFinished = true;
      action.play();
    }
  }

  useFrame((state, delta) => {
    mixer?.update(delta);
    // gltf.scene.rotation.y += delta * 0.05; // 회전 속도를 조절할 수 있습니다.
  });

  function func() {
    setIsClicked(true);
    return;
  }

  return (
    <>
      <primitive
        object={gltf.scene}
        scale={[4, 4, 4]}
        position={[0, -2, 0]}
        rotation={[0, -0.8, 0]}
        onClick={func}
      />
    </>
  );
};
