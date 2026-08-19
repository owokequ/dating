import { StyleSheet, Text, View } from 'react-native';

export default function ExploreScreen() {
  return (
    <View style={styles.screen}>
      <Text style={styles.text}>Экран будет добавлен в следующем этапе разработки.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  text: { color: '#172033', fontSize: 17, textAlign: 'center' },
});
