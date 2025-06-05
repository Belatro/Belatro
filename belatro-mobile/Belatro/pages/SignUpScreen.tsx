import React, { useState } from 'react';
import { SafeAreaView, View, Text, TextInput, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { registerUser } from '../services/userService';
import styles from '../styles/styles';


export default function SignUpScreen({ navigation }: { navigation: any }) {
    const [form, setForm] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [isLoading, setIsLoading] = useState(false);


    const validate = () => {
        if (!form.username.trim()) {
            Alert.alert('Validation', 'Username is required');
            return false;
        }
        if (!form.email.includes('@') || !form.email.includes('.')) {
            Alert.alert('Validation', 'Enter a valid email');
            return false;
        }
        if (form.password.length < 6 || form.password.length > 16) {
            Alert.alert('Validation', 'Password must be 6-16 characters');
            return false;
        }
        if (!/\d/.test(form.password)) {
            Alert.alert('Validation', 'Password must contain at least one number');
            return false;
        }
        if (form.password !== form.confirmPassword) {
            Alert.alert('Validation', 'Passwords do not match');
            return false;
        }
        return true;
    };

    const handleSignUp = async () => {
        if (!validate()) return;
        setIsLoading(true);
        try {
            await registerUser({
                username: form.username,
                email: form.email,
                password: form.password,
            });
            Alert.alert('Success', 'Account created!', [
                { text: 'OK', onPress: () => navigation.navigate('SignIn') },
            ]);
        } catch (error: any) {
            if (error.response?.data?.message) {
                Alert.alert('Error', error.response.data.message);
            } else {
                Alert.alert('Error', 'Network or server error');
            }
        } finally {
            setIsLoading(false);
        }
    };


    return (
        <SafeAreaView style={{ flex: 1, backgroundColor: '#1e1e1e' }}>
            <View style={styles.containerSignUp}>
                <View style={styles.headerSignUp}>
                    <Text style={styles.loginTextSignUp}>Sign up to Belatro</Text>
                </View>
                <View style={styles.formSignUp}>
                    <View style={styles.inputSignUp}>
                        <Text style={styles.inputLabelSignUp}>Username</Text>
                        <TextInput
                            autoCapitalize="none"
                            autoCorrect={false}
                            style={styles.inputControlSignUp}
                            value={form.username}
                            onChangeText={(username) => setForm({ ...form, username })}
                        />
                    </View>
                    <View style={styles.inputSignUp}>
                        <Text style={styles.inputLabelSignUp}>Email</Text>
                        <TextInput
                            autoCapitalize="none"
                            autoCorrect={false}
                            keyboardType="email-address"
                            style={styles.inputControlSignUp}
                            value={form.email}
                            onChangeText={(email) => setForm({ ...form, email })}
                        />
                    </View>
                    <View style={styles.inputSignUp}>
                        <Text style={styles.inputLabelSignUp}>Password</Text>
                        <TextInput
                            secureTextEntry
                            style={styles.inputControlSignUp}
                            value={form.password}
                            onChangeText={(password) => setForm({ ...form, password })}
                        />
                    </View>
                    <View style={styles.inputSignUp}>
                        <Text style={styles.inputLabelSignUp}>Confirm Password</Text>
                        <TextInput
                            secureTextEntry
                            style={styles.inputControlSignUp}
                            value={form.confirmPassword}
                            onChangeText={(confirmPassword) => setForm({ ...form, confirmPassword })}
                        />
                    </View>
                    <View style={styles.formAction}>
                        <TouchableOpacity
                            onPress={handleSignUp}
                            disabled={isLoading}
                            style={[styles.btnSignUp, isLoading && { opacity: 0.5 }]}
                        >
                            <Text style={styles.btnTextSignUp}>{isLoading ? 'Creating...' : 'Sign up'}</Text>
                        </TouchableOpacity>
                        <TouchableOpacity onPress={() => navigation.navigate('SignIn')}>
                            <Text style={styles.formFooterSignUp}>
                                Already have an account?{' '}
                                <Text style={{ textDecorationLine: 'underline' }}>Sign in</Text>
                            </Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </View>
        </SafeAreaView>
    );
}

