<script setup lang="ts">
defineOptions({ name: 'BlankLayout' })
</script>

<template>
  <div class="blank-layout">
    <router-view />
  </div>
</template>

<style scoped>
.blank-layout {
  height: 100%;
}
</style>
